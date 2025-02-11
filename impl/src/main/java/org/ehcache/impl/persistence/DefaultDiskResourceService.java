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

package org.ehcache.impl.persistence;

import org.ehcache.CachePersistenceException;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourceType;
import org.ehcache.config.SizedResourcePool;
import org.ehcache.core.spi.service.DiskResourceService;
import org.ehcache.core.spi.service.FileBasedPersistenceContext;
import org.ehcache.core.spi.service.LocalPersistenceService;
import org.ehcache.core.spi.service.LocalPersistenceService.SafeSpaceIdentifier;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.ehcache.spi.persistence.StateRepository;
import org.ehcache.spi.service.MaintainableService;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceDependencies;
import org.ehcache.spi.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of the {@link DiskResourceService} which can be used explicitly when
 * {@link org.ehcache.PersistentUserManagedCache persistent user managed caches} are desired.
 */
@ServiceDependencies(LocalPersistenceService.class)
public class DefaultDiskResourceService implements DiskResourceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDiskResourceService.class);
  static final String PERSISTENCE_SPACE_OWNER = "file";

  // yukms TODO: 已经创建的持久化空间
  private final ConcurrentMap<String, PersistenceSpace> knownPersistenceSpaces = new ConcurrentHashMap<>();
  private volatile LocalPersistenceService persistenceService;
  private volatile boolean isStarted;

  private boolean isStarted() {
    return isStarted;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start(final ServiceProvider<Service> serviceProvider) {
    innerStart(serviceProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startForMaintenance(ServiceProvider<? super MaintainableService> serviceProvider, MaintenanceScope maintenanceScope) {
    innerStart(serviceProvider);
  }

  private void innerStart(ServiceProvider<? super MaintainableService> serviceProvider) {
    persistenceService = serviceProvider.getService(LocalPersistenceService.class);
    isStarted = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    isStarted = false;
    persistenceService = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlesResourceType(ResourceType<?> resourceType) {
    return persistenceService != null && ResourceType.Core.DISK.equals(resourceType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PersistenceSpaceIdentifier<DiskResourceService> getPersistenceSpaceIdentifier(String name, CacheConfiguration<?, ?> config) throws CachePersistenceException {
    if (persistenceService == null) {
      return null;
    }
    // yukms TODO: org.ehcache.config.ResourcePool.isPersistent
    boolean persistent = config.getResourcePools().getPoolForResource(ResourceType.Core.DISK).isPersistent();
    // yukms TODO: 循环直到创建成功
    while (true) {
      PersistenceSpace persistenceSpace = knownPersistenceSpaces.get(name);
      if (persistenceSpace != null) {
        // yukms TODO: 已经创建直接返回
        return persistenceSpace.identifier;
      }
      // yukms TODO: 没有创建的话需要创建
      PersistenceSpace newSpace = createSpace(name, persistent);
      if (newSpace != null) {
        // yukms TODO: 创建成功则返回
        return newSpace.identifier;
      }
    }
  }

  @Override
  public void releasePersistenceSpaceIdentifier(PersistenceSpaceIdentifier<?> identifier) throws CachePersistenceException {
    String name = null;
    for (Map.Entry<String, PersistenceSpace> entry : knownPersistenceSpaces.entrySet()) {
      if (entry.getValue().identifier.equals(identifier)) {
        name = entry.getKey();
      }
    }
    if (name == null) {
      throw newCachePersistenceException(identifier);
    }
    PersistenceSpace persistenceSpace = knownPersistenceSpaces.remove(name);
    if (persistenceSpace != null) {
      for (FileBasedStateRepository stateRepository : persistenceSpace.stateRepositories.values()) {
        try {
          stateRepository.close();
        } catch (IOException e) {
          LOGGER.warn("StateRepository close failed - destroying persistence space {} to prevent corruption", identifier, e);
          persistenceService.destroySafeSpace(((DefaultPersistenceSpaceIdentifier)identifier).persistentSpaceId, true);
        }
      }
    }
  }

  private PersistenceSpace createSpace(String name, boolean persistent) throws CachePersistenceException {
    // yukms TODO: 获取文件路径
    DefaultPersistenceSpaceIdentifier persistenceSpaceIdentifier =
        new DefaultPersistenceSpaceIdentifier(persistenceService.createSafeSpaceIdentifier(PERSISTENCE_SPACE_OWNER, name));
    PersistenceSpace persistenceSpace = new PersistenceSpace(persistenceSpaceIdentifier);
    // yukms TODO: 并发抢占检测
    if (knownPersistenceSpaces.putIfAbsent(name, persistenceSpace) == null) {
      boolean created = false;
      try {
        if (!persistent) {
          // yukms TODO: 如果资源不是持久化，则尝试销毁该文件夹的内容
          persistenceService.destroySafeSpace(persistenceSpaceIdentifier.persistentSpaceId, true);
        }
        // yukms TODO: 创建文件夹
        persistenceService.createSafeSpace(persistenceSpaceIdentifier.persistentSpaceId);
        created = true;
      } finally {
        if (!created) {
          // this happens only if an exception is thrown..clean up for any throwable..
          // yukms TODO: 只有在抛出异常时才会发生此情况。请清理任何可丢弃项。。
          // yukms TODO: 创建失败丢弃
          knownPersistenceSpaces.remove(name, persistenceSpace);
        }
      }
      return persistenceSpace;
    }
    // yukms TODO: 抢占失败
    return null;
  }

  private void checkStarted() {
    if(!isStarted()) {
      throw new IllegalStateException(getClass().getName() + " should be started to call destroy");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy(String name) {
    // yukms TODO: 检测状态
    checkStarted();

    if(persistenceService == null) {
      return;
    }

    PersistenceSpace space = knownPersistenceSpaces.remove(name);
    // yukms TODO: space == null就直接返回就是了，为什么没有要创建，然后再销毁呢？
    SafeSpaceIdentifier identifier = (space == null) ?
      persistenceService.createSafeSpaceIdentifier(PERSISTENCE_SPACE_OWNER, name) : space.identifier.persistentSpaceId;
    persistenceService.destroySafeSpace(identifier, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroyAll() {
    checkStarted();

    if(persistenceService == null) {
      return;
    }

    persistenceService.destroyAll(PERSISTENCE_SPACE_OWNER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateRepository getStateRepositoryWithin(PersistenceSpaceIdentifier<?> identifier, String name)
      throws CachePersistenceException {

    PersistenceSpace persistenceSpace = getPersistenceSpace(identifier);
    if(persistenceSpace == null) {
      throw newCachePersistenceException(identifier);
    }

    FileBasedStateRepository stateRepository = new FileBasedStateRepository(
        FileUtils.createSubDirectory(persistenceSpace.identifier.persistentSpaceId.getRoot(), name));
    FileBasedStateRepository previous = persistenceSpace.stateRepositories.putIfAbsent(name, stateRepository);
    if (previous != null) {
      return previous;
    }
    return stateRepository;
  }

  private CachePersistenceException newCachePersistenceException(PersistenceSpaceIdentifier<?> identifier) {
    return new CachePersistenceException("Unknown space: " + identifier);
  }

  private PersistenceSpace getPersistenceSpace(PersistenceSpaceIdentifier<?> identifier) {
    for (PersistenceSpace persistenceSpace : knownPersistenceSpaces.values()) {
      if (persistenceSpace.identifier.equals(identifier)) {
        return persistenceSpace;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileBasedPersistenceContext createPersistenceContextWithin(PersistenceSpaceIdentifier<?> identifier, String name)
      throws CachePersistenceException {
    if(getPersistenceSpace(identifier) == null) {
      throw newCachePersistenceException(identifier);
    }
    return new DefaultFileBasedPersistenceContext(
        FileUtils.createSubDirectory(((DefaultPersistenceSpaceIdentifier)identifier).persistentSpaceId.getRoot(), name));
  }

  private static class PersistenceSpace {
    final DefaultPersistenceSpaceIdentifier identifier;
    final ConcurrentMap<String, FileBasedStateRepository> stateRepositories = new ConcurrentHashMap<>();

    private PersistenceSpace(DefaultPersistenceSpaceIdentifier identifier) {
      this.identifier = identifier;
    }
  }

  private static class DefaultPersistenceSpaceIdentifier implements PersistenceSpaceIdentifier<DiskResourceService> {
    final SafeSpaceIdentifier persistentSpaceId;

    private DefaultPersistenceSpaceIdentifier(SafeSpaceIdentifier persistentSpaceId) {
      this.persistentSpaceId = persistentSpaceId;
    }

    @Override
    public Class<DiskResourceService> getServiceType() {
      return DiskResourceService.class;
    }

    @Override
    public String toString() {
      return persistentSpaceId.toString();
    }

    // no need to override equals and hashcode as references are private and created in a protected fashion
    // within this class. So two space identifiers are equal iff their references are equal.
  }

  private static class DefaultFileBasedPersistenceContext implements FileBasedPersistenceContext {
    private final File directory;

    private DefaultFileBasedPersistenceContext(File directory) {
      this.directory = directory;
    }

    @Override
    public File getDirectory() {
      return directory;
    }
  }
}
