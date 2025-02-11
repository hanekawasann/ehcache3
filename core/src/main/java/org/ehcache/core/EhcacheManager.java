/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import javax.naming.ldap.Rdn;

import static org.ehcache.core.spi.ServiceLocator.dependencySet;
import static org.ehcache.core.spi.service.ServiceUtils.findOptionalAmongst;
import static org.ehcache.core.spi.service.ServiceUtils.findSingletonAmongst;
import org.ehcache.Cache;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.Builder;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.config.ResourcePool;
import org.ehcache.config.ResourceType;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.core.config.store.StoreEventSourceConfiguration;
import org.ehcache.core.config.store.StoreStatisticsConfiguration;
import org.ehcache.core.events.CacheEventDispatcher;
import org.ehcache.core.events.CacheEventDispatcherFactory;
import org.ehcache.core.events.CacheEventListenerConfiguration;
import org.ehcache.core.events.CacheEventListenerProvider;
import org.ehcache.core.events.CacheManagerListener;
import org.ehcache.core.resilience.DefaultRecoveryStore;
import org.ehcache.core.spi.LifeCycled;
import org.ehcache.core.spi.LifeCycledAdapter;
import org.ehcache.core.spi.ServiceLocator;
import org.ehcache.core.spi.service.CacheManagerProviderService;
import org.ehcache.core.spi.service.ServiceUtils;
import org.ehcache.core.spi.store.InternalCacheManager;
import org.ehcache.core.spi.store.Store;
import org.ehcache.core.store.StoreConfigurationImpl;
import org.ehcache.core.store.StoreSupport;
import org.ehcache.core.util.ClassLoading;
import org.ehcache.event.CacheEventListener;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.ehcache.spi.loaderwriter.CacheLoaderWriterConfiguration;
import org.ehcache.spi.loaderwriter.CacheLoaderWriterProvider;
import org.ehcache.spi.loaderwriter.WriteBehindProvider;
import org.ehcache.spi.persistence.PersistableResourceService;
import org.ehcache.spi.resilience.ResilienceStrategy;
import org.ehcache.spi.resilience.ResilienceStrategyProvider;
import org.ehcache.spi.serialization.SerializationProvider;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.UnsupportedTypeException;
import org.ehcache.spi.service.MaintainableService;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.spi.service.ServiceCreationConfiguration;
import org.ehcache.spi.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for the {@link org.ehcache.CacheManager} and {@link PersistentCacheManager}
 * <p>
 * {@code Ehcache} users should not have to depend on this type but rely exclusively on the api types in package
 * {@code org.ehcache}.
 */
public class EhcacheManager implements PersistentCacheManager, InternalCacheManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(EhcacheManager.class);

  private final DefaultConfiguration configuration;
  private final ClassLoader cacheManagerClassLoader;

  private final boolean useLoaderInAtomics;
  // yukms TODO: 缓存
  private final ConcurrentMap<String, CacheHolder> caches = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<CacheManagerListener> listeners = new CopyOnWriteArrayList<>();

  private final StatusTransitioner statusTransitioner = new StatusTransitioner(LOGGER);
  private final String simpleName;
  protected final ServiceLocator serviceLocator;

  public EhcacheManager(Configuration config) {
    this(config, Collections.emptyList(), true);
  }

  public EhcacheManager(Configuration config, Collection<Service> services) {
    this(config, services, true);
  }

  public EhcacheManager(Configuration config, Collection<Service> services, boolean useLoaderInAtomics) {
    this(config, dependencies -> dependencies.with(services), useLoaderInAtomics);
  }

  public EhcacheManager(Configuration config, UnaryOperator<ServiceLocator.DependencySet> customization, boolean useLoaderInAtomics) {
    final String simpleName = this.getClass().getSimpleName();
    this.simpleName = (simpleName.isEmpty() ? this.getClass().getName() : simpleName);
    this.configuration = new DefaultConfiguration(config);
    this.cacheManagerClassLoader = config.getClassLoader() != null ? config.getClassLoader() : ClassLoading.getDefaultClassLoader();
    this.useLoaderInAtomics = useLoaderInAtomics;
    validateServicesConfigs();
    // yukms TODO: 重点代码，如何构建服务与服务的依赖关系
    this.serviceLocator = resolveServices(customization);
  }

  private void validateServicesConfigs() {
    Set<Class<?>> classes = new HashSet<>();
    for (ServiceCreationConfiguration<?, ?> service : configuration.getServiceCreationConfigurations()) {
      if (!classes.add(service.getServiceType())) {
        throw new IllegalStateException("Duplicate creation configuration for service " + service.getServiceType());
      }
    }
  }

  private ServiceLocator resolveServices(UnaryOperator<ServiceLocator.DependencySet> customization) {
    // yukms TODO: 默认需要的service
    ServiceLocator.DependencySet builder = dependencySet()
      .with(Store.Provider.class)
      // yukms TODO: OnHeapStoreProviderFactory
      // yukms TODO: OffHeapStoreProviderFactory
      // yukms TODO: OffHeapDiskStoreProviderFactory
      // yukms TODO: TieredStoreProviderFactory
      // yukms TODO: LoaderWriterStoreProviderFactory

      // yukms TODO: TimeSourceService TimeSourceServiceFactory
      // yukms TODO: CopyProvider DefaultCopyProviderFactory
      // yukms TODO: SizeOfEngineProvider DefaultSizeOfEngineProviderFactory
      // yukms TODO: StatisticsService DefaultStatisticsServiceFactory
      // yukms TODO: SerializationProvider DefaultSerializationProviderFactory
      // yukms TODO: DiskResourceService DefaultDiskResourceServiceFactory
      // yukms TODO: CachingTier.Provider CompoundCachingTierProviderFactory
      .with(CacheLoaderWriterProvider.class)// yukms TODO: DefaultCacheLoaderWriterProviderFactory
      .with(WriteBehindProvider.class)// yukms TODO: WriteBehindProviderFactory
      .with(CacheEventDispatcherFactory.class)// yukms TODO: CacheEventNotificationListenerServiceProviderFactory
      .with(CacheEventListenerProvider.class)// yukms TODO: DefaultCacheEventListenerProviderFactory
      .with(ResilienceStrategyProvider.class);// yukms TODO: DefaultResilienceStrategyProviderFactory
    // yukms TODO: ExecutionService DefaultExecutionServiceFactory

    // yukms TODO: builder.with(services)
    builder = customization.apply(builder);

    if (!builder.contains(CacheManagerProviderService.class)) {
      builder = builder.with(new DefaultCacheManagerProviderService(this));
    }
    for (ServiceCreationConfiguration<?, ?> serviceConfig : configuration.getServiceCreationConfigurations()) {
      builder = builder.with(serviceConfig);
    }
    return builder.build();
  }

  /**
   * Exposed for testing purpose
   *
   * @return the status transitioner keeping the current cache manager state
   */
  StatusTransitioner getStatusTransitioner() {
    return statusTransitioner;
  }

  @Override
  public <K, V> Cache<K, V> getCache(String alias, Class<K> keyType, Class<V> valueType) {
    statusTransitioner.checkAvailable();
    final CacheHolder cacheHolder = caches.get(alias);
    if(cacheHolder == null) {
      return null;
    } else {
      try {
        return cacheHolder.retrieve(keyType, valueType);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Cache '" + alias + "' type is <" + cacheHolder.keyType.getName() + ", "
                                           + cacheHolder.valueType.getName() + ">, but you retrieved it with <"
                                           + keyType.getName() + ", " + valueType.getName() +">");
      }
    }
  }

  @Override
  public void removeCache(final String alias) {
    if (alias == null) {
      throw new NullPointerException("Alias cannot be null");
    }
    removeCache(alias, true);
  }

  /**
   * Closes and removes a cache, by alias, from this cache manager.
   *
   * @param alias the alias of the cache to remove
   * @param removeFromConfig if {@code true}, the cache configuration is altered to remove the cache
   */
  private void removeCache(final String alias, final boolean removeFromConfig) {
    statusTransitioner.checkAvailable();
    final CacheHolder cacheHolder = caches.remove(alias);
    if(cacheHolder != null) {
      final InternalCache<?, ?> ehcache = cacheHolder.retrieve(cacheHolder.keyType, cacheHolder.valueType);
      if (ehcache != null) {
        if (removeFromConfig) {
          configuration.removeCacheConfiguration(alias);
        }

        // yukms TODO: 移除CacheManagerListener
        if (!statusTransitioner.isTransitioning()) {
          for (CacheManagerListener listener : listeners) {
            listener.cacheRemoved(alias, ehcache);
          }
        }

        // yukms TODO: 调用关闭方法
        ehcache.close();
        // yukms TODO: 清理持久化数据
        closeEhcache(alias, ehcache);
      }
      LOGGER.info("Cache '{}' removed from {}.", alias, simpleName);
    }
  }

  /**
   * Perform cache closure actions specific to a cache manager implementation.
   * 执行特定于缓存管理器实现的缓存关闭操作。
   * This method is called <i>after</i> the {@code InternalCache} instance is closed.
   * 在关闭{@code InternalCache}实例后，<i>调用此方法。
   *
   * @param alias the cache alias
   * @param ehcache the {@code InternalCache} instance for the cache to close
   */
  protected void closeEhcache(final String alias, final InternalCache<?, ?> ehcache) {
    for (ResourceType<?> resourceType : ehcache.getRuntimeConfiguration().getResourcePools().getResourceTypeSet()) {
      if (resourceType.isPersistable()) {
        ResourcePool resourcePool = ehcache.getRuntimeConfiguration()
            .getResourcePools()
            .getPoolForResource(resourceType);
        // yukms TODO: org.ehcache.config.ResourceType.isPersistable与org.ehcache.config.ResourcePool.isPersistent代表的意义不一样，需要debug
        if (!resourcePool.isPersistent()) {
          PersistableResourceService persistableResourceService = getPersistableResourceService(resourceType);
          try {
            persistableResourceService.destroy(alias);
          } catch (CachePersistenceException e) {
            LOGGER.warn("Unable to clear persistence space for cache {}", alias, e);
          }
        }
      }
    }
  }

  @Override
  public <K, V> Cache<K, V> createCache(String alias, Builder<? extends CacheConfiguration<K, V>> configBuilder) {
    return createCache(alias, configBuilder.build());
  }

  @Override
  public <K, V> Cache<K, V> createCache(final String alias, CacheConfiguration<K, V> config) throws IllegalArgumentException {
    return createCache(alias, config, true);
  }

  // yukms TODO: 重点代码
  private <K, V> Cache<K, V> createCache(String alias, CacheConfiguration<K, V> originalConfig, boolean addToConfig) throws IllegalArgumentException {
    statusTransitioner.checkAvailable();

    LOGGER.debug("Creating Cache '{}' in {}.", alias, simpleName);

    // yukms TODO: 配置调整
    CacheConfiguration<K, V> config = adjustConfigurationWithCacheManagerDefaults(alias, originalConfig);
    Class<K> keyType = config.getKeyType();
    Class<V> valueType = config.getValueType();

    // yukms TODO: 创建CacheHolder
    CacheHolder value = new CacheHolder(keyType, valueType);
    if (caches.putIfAbsent(alias, value) != null) {
      throw new IllegalArgumentException("Cache '" + alias +"' already exists");
    }

    // yukms TODO: 创建InternalCache
    InternalCache<K, V> cache = null;

    boolean success = false;
    RuntimeException failure = null;
    try {
      cache = createNewEhcache(alias, config, keyType, valueType);
      // yukms TODO: 初始化
      cache.init();
      if (addToConfig) {
        configuration.addCacheConfiguration(alias, cache.getRuntimeConfiguration());
      } else {
        configuration.replaceCacheConfiguration(alias, originalConfig, cache.getRuntimeConfiguration());
      }
      success = true;
    } catch (RuntimeException e) {
      failure = e;
    } finally {
      // yukms TODO: 创建失败
      if (!success) {
        caches.remove(alias);
        value.setCache(null);
      }
    }

    // yukms TODO: 创建监听
    if(failure == null) {
      try {
        // yukms TODO: 再次检测
        if(!statusTransitioner.isTransitioning()) {
          for (CacheManagerListener listener : listeners) {
            listener.cacheAdded(alias, cache);
          }
        }
      } finally {
        value.setCache(cache);
      }
    } else {
      throw new IllegalStateException("Cache '"+alias+"' creation in " + simpleName +
          " failed.", failure);
    }
    LOGGER.info("Cache '{}' created in {}.", alias, simpleName);
    return cache;
  }

  <K, V> InternalCache<K, V> createNewEhcache(String alias, CacheConfiguration<K, V> config,
                                        Class<K> keyType, Class<V> valueType) {
    Collection<ServiceConfiguration<?, ?>> adjustedServiceConfigs = new ArrayList<>(config.getServiceConfigurations());

    // yukms TODO: 检测无法识别的ServiceConfiguration
    List<ServiceConfiguration<?, ?>> unknownServiceConfigs = new ArrayList<>();
    for (ServiceConfiguration<?, ?> serviceConfig : adjustedServiceConfigs) {
      if (!serviceLocator.knowsServiceFor(serviceConfig)) {
        unknownServiceConfigs.add(serviceConfig);
      }
    }
    if (!unknownServiceConfigs.isEmpty()) {
      throw new IllegalStateException("Cannot find service(s) that can handle following configuration(s) : " + unknownServiceConfigs);
    }

    // yukms TODO: CacheLoaderWriter
    List<LifeCycled> lifeCycledList = new ArrayList<>();

    CacheLoaderWriterProvider cacheLoaderWriterProvider = serviceLocator.getService(CacheLoaderWriterProvider.class);
    CacheLoaderWriter<? super K, V> loaderWriter;
    if(cacheLoaderWriterProvider != null) {
      // yukms TODO: 创建CacheLoaderWriter，默认是没有配置的，所以这里创建结果为null
      loaderWriter = cacheLoaderWriterProvider.createCacheLoaderWriter(alias, config);

      if (loaderWriter != null) {
        lifeCycledList.add(new LifeCycledAdapter() {
          @Override
          public void close() throws Exception {
            // yukms TODO: 释放CacheLoaderWriter
            cacheLoaderWriterProvider.releaseCacheLoaderWriter(alias, loaderWriter);
          }
        });
      }
    } else {
      loaderWriter = null;
    }

    // yukms TODO: 创建Store
    Store<K, V> store = getStore(alias, config, keyType, valueType, adjustedServiceConfigs, lifeCycledList, loaderWriter);

    // yukms TODO: CacheEventDispatcher
    CacheEventDispatcherFactory cenlProvider = serviceLocator.getService(CacheEventDispatcherFactory.class);
    // yukms TODO: 创建CacheEventDispatcher org.ehcache.impl.events.CacheEventDispatcherImpl
    CacheEventDispatcher<K, V> evtService =
        cenlProvider.createCacheEventDispatcher(store, adjustedServiceConfigs.toArray(new ServiceConfiguration<?, ?>[adjustedServiceConfigs.size()]));
    lifeCycledList.add(new LifeCycledAdapter() {
      @Override
      public void close() {
        // yukms TODO: 释放CacheEventDispatcher
        cenlProvider.releaseCacheEventDispatcher(evtService);
      }
    });
    evtService.setStoreEventSource(store.getStoreEventSource());

    // yukms TODO: 创建ResilienceStrategy
    ResilienceStrategyProvider resilienceProvider = serviceLocator.getService(ResilienceStrategyProvider.class);
    ResilienceStrategy<K, V> resilienceStrategy;
    if (loaderWriter == null) {
      resilienceStrategy = resilienceProvider.createResilienceStrategy(alias, config, new DefaultRecoveryStore<>(store));
    } else {
      resilienceStrategy = resilienceProvider.createResilienceStrategy(alias, config, new DefaultRecoveryStore<>(store), loaderWriter);
    }

    // yukms TODO: InternalCache
    InternalCache<K, V> cache = new Ehcache<>(config, store, resilienceStrategy, evtService, LoggerFactory.getLogger(Ehcache.class + "-" + alias), loaderWriter);

    // yukms TODO: CacheEventListener org.ehcache.core.CacheConfigurationChangeListener
    CacheEventListenerProvider evntLsnrFactory = serviceLocator.getService(CacheEventListenerProvider.class);
    if (evntLsnrFactory != null) {
      @SuppressWarnings("unchecked")
      Collection<CacheEventListenerConfiguration<?>> evtLsnrConfigs =
          ServiceUtils.<Class<CacheEventListenerConfiguration<?>>>findAmongst((Class) CacheEventListenerConfiguration.class, config.getServiceConfigurations());
      for (CacheEventListenerConfiguration<?> lsnrConfig: evtLsnrConfigs) {
        // yukms TODO: 创建CacheEventListener
        CacheEventListener<K, V> lsnr = evntLsnrFactory.createEventListener(alias, lsnrConfig);
        if (lsnr != null) {
          // yukms TODO: 注册事件监听器
          cache.getRuntimeConfiguration().registerCacheEventListener(lsnr, lsnrConfig.orderingMode(), lsnrConfig.firingMode(),
              lsnrConfig.fireOn());
          lifeCycledList.add(new LifeCycled() {
            @Override
            public void init() {
              // no-op for now
            }

            @Override
            public void close() throws Exception {
              // yukms TODO: 释放CacheEventListener
              evntLsnrFactory.releaseEventListener(lsnr);
            }
          });
        }
      }
      evtService.setListenerSource(cache);
    }

    // yukms TODO: 将所有的声明周期钩子放到cache中
    for (LifeCycled lifeCycled : lifeCycledList) {
      cache.addHook(lifeCycled);
    }

    return cache;
  }

  /**
   * Instantiates a {@code Store} used for the cache data.
   *
   * @param alias the alias assigned to the cache
   * @param config the configuration used for the cache
   * @param keyType the cache key type
   * @param valueType the cache value type
   * @param serviceConfigs the {@code List} of {@code ServiceConfiguration} instances available to the cache;
   *                       this list may be augmented by the implementation of this method
   * @param lifeCycledList the {@code List} of {@code LifeCycled} instances used to manage components of the
   *                       cache; this list may be augmented by the implementation of this method
   * @param loaderWriter the {@code CacheLoaderWriter} to be used by the {@code Cache}
   * @param <K> the cache key type
   * @param <V> the cache value type
   *
   * @return the {@code Store} instance used to create the cache
   */
  protected <K, V> Store<K, V> getStore(String alias, CacheConfiguration<K, V> config,
                                       Class<K> keyType, Class<V> valueType,
                                       Collection<ServiceConfiguration<?, ?>> serviceConfigs,
                                       List<LifeCycled> lifeCycledList, CacheLoaderWriter<? super K, V> loaderWriter) {
    // yukms TODO: 容量配置
    final Set<ResourceType<?>> resourceTypes = config.getResourcePools().getResourceTypeSet();
    for (ResourceType<?> resourceType : resourceTypes) {
      if (resourceType.isPersistable()) {
        final PersistableResourceService persistableResourceService = getPersistableResourceService(resourceType);

        try {
          // yukms TODO: 创建PersistenceSpaceIdentifier
          final PersistableResourceService.PersistenceSpaceIdentifier<?> spaceIdentifier = persistableResourceService
              .getPersistenceSpaceIdentifier(alias, config);
          // yukms TODO: 为什么作为配置项加入
          serviceConfigs.add(spaceIdentifier);
          lifeCycledList.add(new LifeCycledAdapter() {
            @Override
            public void close() throws Exception {
              // yukms TODO: 释放PersistenceSpaceIdentifier
              persistableResourceService.releasePersistenceSpaceIdentifier(spaceIdentifier);
            }
          });
        } catch (CachePersistenceException e) {
          throw new RuntimeException("Unable to handle persistence", e);
        }
      }
    }

    // yukms TODO: key与value的序列化器
    Serializer<K> keySerializer = null;
    Serializer<V> valueSerializer = null;
    // yukms TODO: 是不是如果没有使用磁盘等需要序列化的缓存就不会有SerializationProvider
    final SerializationProvider serialization = serviceLocator.getService(SerializationProvider.class);
    ServiceConfiguration<?, ?>[] serviceConfigArray = serviceConfigs.toArray(new ServiceConfiguration<?, ?>[serviceConfigs.size()]);
    if (serialization != null) {
      try {
        // yukms TODO: 创建序列化器
        final Serializer<K> keySer = serialization.createKeySerializer(keyType, config.getClassLoader(), serviceConfigArray);
        lifeCycledList.add(new LifeCycledAdapter() {
          @Override
          public void close() throws Exception {
            // yukms TODO: 释放序列化器
            serialization.releaseSerializer(keySer);
          }
        });
        keySerializer = keySer;
      } catch (UnsupportedTypeException e) {
        // yukms TODO: 抛出位置org.ehcache.impl.internal.spi.serialization.DefaultSerializationProvider.getSerializerClassFor
        // yukms TODO: 不支持的序列化类型。自定义中既没有Class，也没有实例化
        for (ResourceType<?> resource : resourceTypes) {
          // yukms TODO: 检测如果资源类型中有必须要序列化的则抛出错误
          if (resource.requiresSerialization()) {
            throw new RuntimeException(e);
          }
        }
        LOGGER.debug("Could not create serializers for {}", alias, e);
      }
      // yukms TODO: 逻辑一致
      try {
        final Serializer<V> valueSer = serialization.createValueSerializer(valueType, config.getClassLoader(), serviceConfigArray);
        lifeCycledList.add(new LifeCycledAdapter() {
          @Override
          public void close() throws Exception {
            serialization.releaseSerializer(valueSer);
          }
        });
        valueSerializer = valueSer;
      } catch (UnsupportedTypeException e) {
        for (ResourceType<?> resource : resourceTypes) {
          if (resource.requiresSerialization()) {
            throw new RuntimeException(e);
          }
        }
        LOGGER.debug("Could not create serializers for {}", alias, e);
      }
    }

    Collection<ServiceConfiguration<?, ?>> serviceConfigurations = config.getServiceConfigurations();

    // yukms TODO: StoreEventSourceConfiguration
    @SuppressWarnings("unchecked")
    int dispatcherConcurrency = findOptionalAmongst((Class<StoreEventSourceConfiguration<?>>) (Class) StoreEventSourceConfiguration.class, serviceConfigurations)
      .map(StoreEventSourceConfiguration::getDispatcherConcurrency)
      .orElse(StoreEventSourceConfiguration.DEFAULT_DISPATCHER_CONCURRENCY);

    // yukms TODO: StoreStatisticsConfiguration
    boolean operationStatisticsEnabled = findOptionalAmongst(StoreStatisticsConfiguration.class, serviceConfigurations)
      .map(StoreStatisticsConfiguration::isOperationStatisticsEnabled)
      // By default, we enable statistics only in a tiered environment
      // yukms TODO: 默认情况下，我们仅在分层环境中启用统计信息
      .orElseGet(() -> config.getResourcePools().getResourceTypeSet().size() > 1);

    Store.Configuration<K, V> storeConfiguration = new StoreConfigurationImpl<>(config, dispatcherConcurrency,
      operationStatisticsEnabled, keySerializer, valueSerializer, loaderWriter, useLoaderInAtomics);

    // yukms TODO: org.ehcache.core.spi.store.WrapperStore.Provider
    Store.Provider storeProvider = StoreSupport.selectWrapperStoreProvider(serviceLocator, serviceConfigs);
    if (storeProvider == null) {
      // yukms TODO: org.ehcache.core.spi.store.Store.Provider
      storeProvider = StoreSupport.selectStoreProvider(serviceLocator, resourceTypes, serviceConfigs);
    }

    // yukms TODO: 创建Store
    Store<K, V> store = storeProvider.createStore(storeConfiguration, serviceConfigArray);

    AtomicReference<Store.Provider> storeProviderRef = new AtomicReference<>(storeProvider);

    lifeCycledList.add(new LifeCycled() {
      @Override
      public void init() {
        // yukms TODO: 初始化Store
        storeProviderRef.get().initStore(store);
      }

      @Override
      public void close() {
        storeProviderRef.get().releaseStore(store);
      }
    });

    return store;
  }

  private PersistableResourceService getPersistableResourceService(ResourceType<?> resourceType) {
    Collection<PersistableResourceService> services = serviceLocator.getServicesOfType(PersistableResourceService.class);
    for (PersistableResourceService service : services) {
      if (service.handlesResourceType(resourceType)) {
        return service;
      }
    }
    throw new IllegalStateException("No service found for persistable resource: " + resourceType);
  }

  /**
   *  adjusts the config to reflect new classloader & serialization provider
   *  调整配置以反映新的类加载器和序列化提供程序
   */
  private <K, V> CacheConfiguration<K, V> adjustConfigurationWithCacheManagerDefaults(String alias, CacheConfiguration<K, V> config) {
    // yukms TODO: classloader
    if (config.getClassLoader() == null && cacheManagerClassLoader != null) {
      config = config.derive().withClassLoader(cacheManagerClassLoader).build();
    }

    // yukms TODO: serialization provider？？？
    // yukms TODO: 查询当前缓存的配置
    CacheLoaderWriterConfiguration<?> loaderWriterConfiguration = findSingletonAmongst(CacheLoaderWriterConfiguration.class, config.getServiceConfigurations());
    // yukms TODO: 当前缓存没有该配置
    if (loaderWriterConfiguration == null) {
      // yukms TODO: 查询上一个缓存的配置
      CacheLoaderWriterProvider loaderWriterProvider = serviceLocator.getService(CacheLoaderWriterProvider.class);
      CacheLoaderWriterConfiguration<?> preConfiguredCacheLoaderWriterConfig = loaderWriterProvider.getPreConfiguredCacheLoaderWriterConfig(alias);
      if (preConfiguredCacheLoaderWriterConfig != null) {
        // yukms TODO: 上一个缓存的配置则复制该配置
        config = config.derive().withService(preConfiguredCacheLoaderWriterConfig).build();
      }
      // yukms TODO: jsr？？？
      if (loaderWriterProvider.isLoaderJsrProvided(alias)) {
        config = config.derive().withService(new CacheLoaderWriterConfiguration<Void>() {}).build();
      }
    }

    return config;
  }

  @Override
  public void registerListener(CacheManagerListener listener) {
    if(!listeners.contains(listener)) {
      listeners.add(listener);
      statusTransitioner.registerListener(listener);
    }
  }

  @Override
  public void deregisterListener(CacheManagerListener listener) {
    if(listeners.remove(listener)) {
      statusTransitioner.deregisterListener(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() {
    // yukms TODO: 重点代码
    final StatusTransitioner.Transition st = statusTransitioner.init();
    try {
      // yukms TODO: 启动每个service
      serviceLocator.startAllServices();

      // yukms TODO: 提前创建缓存
      Deque<String> initiatedCaches = new ArrayDeque<>();
      try {
        for (Map.Entry<String, CacheConfiguration<?, ?>> cacheConfigurationEntry : configuration.getCacheConfigurations()
            .entrySet()) {
          final String alias = cacheConfigurationEntry.getKey();
          createCache(alias, cacheConfigurationEntry.getValue(), false);
          initiatedCaches.push(alias);
        }
      } catch (RuntimeException e) {
        while (!initiatedCaches.isEmpty()) {
          String toBeClosed = initiatedCaches.pop();
          try {
            removeCache(toBeClosed, false);
          } catch (Exception exceptionClosingCache) {
            LOGGER.error("Cache '{}' could not be removed after initialization failure due to ", toBeClosed, exceptionClosingCache);
          }
        }
        try {
          // yukms TODO: 如果创建失败则停止每个service
          serviceLocator.stopAllServices();
        } catch (Exception exceptionStoppingServices) {
          LOGGER.error("Stopping services after initialization failure failed due to ", exceptionStoppingServices);
        }
        throw e;
      }
      // yukms TODO: UNINITIALIZED -> AVAILABLE完成
      st.succeeded();
    } catch (Exception e) {
      throw st.failed(e);
    } finally {
      st.failed(null);
    }
  }

  @Override
  public Status getStatus() {
    return statusTransitioner.currentStatus();
  }

  @Override
  public void close() {
    final StatusTransitioner.Transition st = statusTransitioner.close();

    Exception firstException = null;
    try {
      for (String alias : caches.keySet()) {
        try {
          removeCache(alias, false);
        } catch (Exception e) {
          if(firstException == null) {
            firstException = e;
          } else {
            LOGGER.error("Cache '{}' could not be removed due to ", alias, e);
          }
        }
      }

      serviceLocator.stopAllServices();
      if (firstException == null) {
        st.succeeded();
      }
    } catch (Exception e) {
      if(firstException == null) {
        firstException = e;
      }
    } finally {
      if(firstException != null) {
        throw st.failed(firstException);
      }
      st.failed(null);
    }
  }

  @Override
  public Configuration getRuntimeConfiguration() {
    return configuration;
  }

  @Override
  public void destroyCache(final String alias) throws CachePersistenceException {
    if (alias == null) {
      throw new NullPointerException("Alias cannot be null");
    }

    LOGGER.debug("Destroying Cache '{}' in {}.", alias, simpleName);

    StatusTransitioner.Transition maintenance = null;
    try {
      // yukms TODO: 进入维护状态
      maintenance = statusTransitioner.maintenance();
    } catch(IllegalStateException e) {
      // the cache manager is already started, no need to put it in maintenance
      // however, we need to check that some other thread ISN'T in maintenance
      // Note that right after the check, there is a window for someone to go in maintenance
      // yukms TODO: 如果不先调用close方法那么会报错，但是依旧能
      statusTransitioner.checkAvailable();
    }

    if(maintenance != null) {
      try {
        // yukms TODO: 准备维护
        startMaintainableServices(MaintainableService.MaintenanceScope.CACHE);
        // yukms TODO: 维护状态进入成功
        maintenance.succeeded();
      } catch (Throwable t) {
        throw maintenance.failed(t);
      }
    }

    try {
      // yukms TODO: 移除缓存
      removeCache(alias, true);
      // yukms TODO: 清理持久化空间
      destroyPersistenceSpace(alias);
    } finally {
      // if it was started, stop it
      if(maintenance != null) {
        // yukms TODO: 退出维护状态
        StatusTransitioner.Transition st = statusTransitioner.exitMaintenance();
        try {
          // yukms TODO: 停止维护
          stopMaintainableServices();
          // yukms TODO: 退出维护状态成功
          st.succeeded();
        } catch (Throwable t) {
          throw st.failed(t);
        }
      }
    }

    LOGGER.info("Cache '{}' successfully destroyed in {}.", alias, simpleName);
  }

  private void destroyPersistenceSpace(String alias) throws CachePersistenceException {
    Collection<PersistableResourceService> services = serviceLocator.getServicesOfType(PersistableResourceService.class);
    for (PersistableResourceService service : services) {
      service.destroy(alias);
    }
  }

  @Override
  public void destroy() throws CachePersistenceException {
    // yukms TODO: 进入维护状态
    StatusTransitioner.Transition st = statusTransitioner.maintenance();
    try {
      // yukms TODO: 准备维护服务
      startMaintainableServices(MaintainableService.MaintenanceScope.CACHE_MANAGER);
      // yukms TODO: 进入维护状态成功
      st.succeeded();
    } catch (Throwable t) {
      throw st.failed(t);
    }

    // yukms TODO: 清理所有持久化空间
    destroyInternal();
    // yukms TODO: 退出维护状态
    st = statusTransitioner.exitMaintenance();
    try {
      // yukms TODO: 停止维护服务
      stopMaintainableServices();
      // yukms TODO: 退出维护状态成功
      st.succeeded();
    } catch (Throwable t) {
      throw st.failed(t);
    }
    LOGGER.info("All persistent data destroyed for {}", simpleName);
  }

  private void startMaintainableServices(MaintainableService.MaintenanceScope maintenanceScope) {
    ServiceProvider<MaintainableService> provider = getMaintainableServiceProvider();
    Collection<MaintainableService> services = serviceLocator.getServicesOfType(MaintainableService.class);
    // yukms TODO: 所有MaintainableService准备维护
    for (MaintainableService service : services) {
      service.startForMaintenance(provider, maintenanceScope);
    }
  }

  private ServiceProvider<MaintainableService> getMaintainableServiceProvider() {
    return new ServiceProvider<MaintainableService>() {
      @Override
      public <U extends MaintainableService> U getService(Class<U> serviceType) {
        return serviceLocator.getService(serviceType);
      }
      @Override
      public <U extends MaintainableService> Collection<U> getServicesOfType(final Class<U> serviceType) {
        return serviceLocator.getServicesOfType(serviceType);
      }
    };
  }

  private void stopMaintainableServices() {
    Collection<MaintainableService> services = serviceLocator.getServicesOfType(MaintainableService.class);
    for (MaintainableService service : services) {
      service.stop();
    }
  }

  // for tests at the moment
  ClassLoader getClassLoader() {
    return cacheManagerClassLoader;
  }

  void destroyInternal() throws CachePersistenceException {
    statusTransitioner.checkMaintenance();
    Collection<PersistableResourceService> services = serviceLocator.getServicesOfType(PersistableResourceService.class);
    for (PersistableResourceService service : services) {
      service.destroyAll();
    }
  }


  private static final class CacheHolder {
    private final Class<?> keyType;
    private final Class<?> valueType;
    private volatile InternalCache<?, ?> cache;
    private volatile boolean isValueSet = false;

    CacheHolder(Class<?> keyType, Class<?> valueType) {
      this.keyType = keyType;
      this.valueType = valueType;
    }

    <K, V> InternalCache<K, V> retrieve(Class<K> refKeyType, Class<V> refValueType) {
      // yukms TODO: 防止cache还没设置完成就被取到
      if (!isValueSet) {
        synchronized (this) {
          boolean interrupted = false;
          try {
            while(!isValueSet) {
              try {
                wait();
              } catch (InterruptedException e) {
                interrupted = true;
              }
            }
          } finally {
            if(interrupted) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
      if (keyType == refKeyType && valueType == refValueType) {
        return cast(cache);
      } else {
        throw new IllegalArgumentException();
      }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> InternalCache<K, V> cast(InternalCache<?, ?> cache) {
      return (InternalCache<K, V>)cache;
    }

    public synchronized void setCache(final InternalCache<?, ?> cache) {
      this.cache = cache;
      this.isValueSet = true;
      notifyAll();
    }
  }
}
