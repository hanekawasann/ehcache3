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

package org.ehcache.core.spi.store.tiering;

import org.ehcache.config.ResourceType;
import org.ehcache.spi.resilience.StoreAccessException;
import org.ehcache.core.spi.store.ConfigurationChangeSupport;
import org.ehcache.core.spi.store.Store;
import org.ehcache.spi.service.PluralService;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * Caching tier is the abstraction for tiers sitting atop the {@link AuthoritativeTier}.
 * 缓存层是位于{@link AuthoritativeTier}之上的层的抽象。
 * <p>
 * As soon as there is more than one tier in a {@link Store}, one will be the {@link AuthoritativeTier} while others
 * will be regrouped under the {@code CachingTier}
 * 一旦{@link Store}中有多个层，其中一层将成为{@link AuthoritativeTier}，而其他层将在{@code CachingTier}下重新组合
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface CachingTier<K, V> extends ConfigurationChangeSupport {

  /**
   * Either return the value holder currently in the caching tier, or compute and store it when it isn't present.
   * 返回缓存层中当前的值持有者，或者在不存在时计算并存储它。
   * <p>
   * Note that in case of expired value holders, {@code null} will be returned and the mapping will be invalidated.
   * 注意，对于过期的值持有者，{@code null}将被返回，映射将无效。
   *
   * @param key the key
   * @param source the function that computes the value when absent from this tier
   *
   * @return the value holder, or {@code null}
   *
   * @throws StoreAccessException if the mapping cannot be retrieved or stored
   */
  Store.ValueHolder<V> getOrComputeIfAbsent(K key, Function<K, Store.ValueHolder<V>> source) throws StoreAccessException;

  /**
   * Either return the value holder currently in the caching tier, or return the provided default.
   * 返回当前缓存层中的值持有者，或返回提供的默认值。
   * <p>
   * Note that in case of expired value holders, {@code null} will be returned and the mapping will be invalidated.
   * 注意，对于过期的值持有者，{@code null}将被返回，映射将无效。
   *
   * @param key the key
   * @param source the function that computes the default value when absent from this tier
   *
   * @return the value holder, or {@code null}
   *
   * @throws StoreAccessException if the mapping cannot be retrieved or stored
   */
  Store.ValueHolder<V> getOrDefault(K key, Function<K, Store.ValueHolder<V>> source) throws StoreAccessException;

  /**
   * Removes a mapping, triggering the {@link InvalidationListener} if registered.
   * 删除映射，如果已注册，则触发{@link InvalidationListener}。
   *
   * @param key the key to remove
   *
   * @throws StoreAccessException if the mapping cannot be removed
   */
  void invalidate(K key) throws StoreAccessException;

  /**
   * Empties the {@code CachingTier}, triggering the {@link InvalidationListener} if registered.
   * 清空{@code CachingTier}，如果已注册，则触发{@link InvalidationListener}。
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void invalidateAll() throws StoreAccessException;

  /**
   * Remove all mappings whose key have the specified hash code from the {@code CachingTier}, triggering the
   * {@link InvalidationListener} if registered.
   * 从{@code CachingTier}中删除其密钥具有指定哈希代码的所有映射，如果已注册，则触发{@link InvalidationListener}。
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void invalidateAllWithHash(long hash) throws StoreAccessException;

  /**
   * Empty out the caching tier.
   * <p>
   * Note that this operation is not atomic.
   * 清空缓存层<p> 请注意，此操作不是原子操作。
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void clear() throws StoreAccessException;

  /**
   * Set the caching tier's {@link InvalidationListener}.
   * 设置缓存层的{@link InvalidationListener}。
   *
   * @param invalidationListener the listener
   */
  void setInvalidationListener(InvalidationListener<K, V> invalidationListener);

  /**
   * Caching tier invalidation listener.
   * <p>
   * Used to notify the {@link AuthoritativeTier} when a mapping is removed so that it can be flushed.
   * 缓存层无效侦听器<p> 用于在删除映射时通知{@link AuthoritativeTier}，以便刷新映射。
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  interface InvalidationListener<K, V> {

    /**
     * Notification that a mapping was evicted or has expired.
     * 映射被逐出或已过期的通知。
     *
     * @param key the mapping's key
     * @param valueHolder the invalidated mapping's value holder
     */
    void onInvalidation(K key, Store.ValueHolder<V> valueHolder);

  }

  /**
   * {@link Service} interface for providing {@link CachingTier} instances.
   * {@link Service}接口，用于提供{@link CachingTier}实例。
   * <p>
   * Multiple providers may exist in a single {@link org.ehcache.CacheManager}.
   * 单个{@link org.ehcache.CacheManager}中可能存在多个提供程序。
   */
  @PluralService
  interface Provider extends Service {

    /**
     * Creates a new {@link CachingTier} instance using the provided configuration
     * 使用提供的配置创建一个新的{@link CachingTier}实例
     *
     * @param storeConfig the {@code Store} configuration
     * @param serviceConfigs a collection of service configurations
     * @param <K> the key type for this tier
     * @param <V> the value type for this tier
     *
     * @return the new caching tier
     */
    <K, V> CachingTier<K, V> createCachingTier(Store.Configuration<K, V> storeConfig, ServiceConfiguration<?, ?>... serviceConfigs);

    /**
     * Releases a {@link CachingTier}.
     * 释放一个{@link CachingTier}。
     *
     * @param resource the caching tier to release
     *
     * @throws IllegalArgumentException if this provider does not know about this caching tier
     */
    void releaseCachingTier(CachingTier<?, ?> resource);

    /**
     * Initialises a {@link CachingTier}.
     * 初始化一个{@link CachingTier}。
     *
     * @param resource the caching tier to initialise
     */
    void initCachingTier(CachingTier<?, ?> resource);

    /**
     * Gets the internal ranking for the {@link CachingTier} instances provided by this {@code Provider} of the
     * caching tier's ability to handle the specified resources.
     * 获取缓存层处理指定资源的能力的{@code Provider}提供的{@link CachingTier}实例的内部排名。
     * <p>
     * A higher rank value indicates a more capable {@code CachingTier}.
     * 秩值越高表示{@code CachingTier}的能力越强。
     *
     * @param resourceTypes the set of {@code ResourceType}s for the store to handle
     * @param serviceConfigs the collection of {@code ServiceConfiguration} instances that may contribute
     *                       to the ranking
     *
     * @return a non-negative rank indicating the ability of a {@code CachingTier} created by this {@code Provider}
     *      to handle the resource types specified by {@code resourceTypes}; a rank of 0 indicates the caching tier
     *      can not handle the type specified in {@code resourceTypes}
     */
    int rankCachingTier(Set<ResourceType<?>> resourceTypes, Collection<ServiceConfiguration<?, ?>> serviceConfigs);
  }

}
