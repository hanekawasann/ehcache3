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

import org.ehcache.core.spi.store.ConfigurationChangeSupport;
import org.ehcache.core.spi.store.Store;
import org.ehcache.spi.resilience.StoreAccessException;
import org.ehcache.spi.service.PluralService;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;

import java.util.function.Function;

/**
 * Interface for the lower tier of a multi-tier {@link CachingTier}.
 * 多层{@link CachingTier}较低层的接口。
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface LowerCachingTier<K, V> extends ConfigurationChangeSupport {

  /**
   * Either return the {@link org.ehcache.core.spi.store.Store.ValueHolder} currently in the caching tier
   * or installs and returns the result of the passed in function.
   * 返回当前缓存层中的{@link org.ehcache.core.spi.store.Store.ValueHolder}，或者安装并返回传入函数的结果。
   * <p>
   * Note that in case of expired {@link org.ehcache.core.spi.store.Store.ValueHolder} {@code null} will be returned
   * and the mapping will be invalidated.
   * 注意，在过期的情况下{@link org.ehcache.core.spi.store.Store.ValueHolder}{@code null}将被返回，映射将无效。
   *
   * @param key the key
   * @param source the function that computes the value
   * @return the value holder, or {@code null}
   *
   * @throws StoreAccessException if the mapping cannot be accessed, installed or removed
   */
  Store.ValueHolder<V> installMapping(K key, Function<K, Store.ValueHolder<V>> source) throws StoreAccessException;

  /**
   * Return the value holder currently in this tier.
   * 返回当前在此层中的值持有者。
   *
   * @param key the key
   * @return the value holder, or {@code null}
   *
   * @throws StoreAccessException if the mapping cannot be access
   */
  Store.ValueHolder<V> get(K key) throws StoreAccessException;

  /**
   * Return the value holder currently in this tier and removes it atomically.
   * 返回当前在此层中的值持有者，并以原子方式将其删除。
   *
   * @param key the key
   * @return the value holder, or {@code null}
   *
   * @throws StoreAccessException if the mapping cannot be access or removed
   */
  Store.ValueHolder<V> getAndRemove(K key) throws StoreAccessException;

  /**
   * Removes a mapping, triggering the {@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener} if
   * registered.
   * 删除映射，如果已注册，则触发{@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener}。
   *
   * @param key the key to remove
   *
   * @throws StoreAccessException if the mapping cannot be removed
   */
  void invalidate(K key) throws StoreAccessException;

  /**
   * Invalidates all mapping, invoking the {@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener} if
   * registered.
   * 使所有映射无效，如果已注册，则调用{@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener}。
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void invalidateAll() throws StoreAccessException;

  /**
   * Invalidates all mappings whose key's hash code matches the provided one, invoking the
   * {@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener} if registered.
   * 如果已注册，则调用{@link org.ehcache.core.spi.store.tiering.CachingTier.InvalidationListener}，使其密钥的哈希代码与提供的哈希代码匹配的所有映射无效。
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void invalidateAllWithHash(long hash) throws StoreAccessException;

  /**
   * Empty out this tier
   *
   * @throws StoreAccessException if mappings cannot be removed
   */
  void clear() throws StoreAccessException;

  /**
   * Set the caching tier's invalidation listener.
   *
   * @param invalidationListener the listener
   */
  void setInvalidationListener(CachingTier.InvalidationListener<K, V> invalidationListener);

  /**
   * {@link Service} interface for providing {@link LowerCachingTier} instances.
   */
  @PluralService
  interface Provider extends Service {

    /**
     * Creates a new {@link LowerCachingTier} instance using the provided configuration
     *
     * @param storeConfig the {@code Store} configuration
     * @param serviceConfigs a collection of service configurations
     * @param <K> the key type for this tier
     * @param <V> the value type for this tier
     *
     * @return the new lower caching tier
     */
    <K, V> LowerCachingTier<K, V> createCachingTier(Store.Configuration<K, V> storeConfig, ServiceConfiguration<?, ?>... serviceConfigs);

    /**
     * Releases a {@link LowerCachingTier}.
     *
     * @param resource the lower caching tier to release
     *
     * @throws IllegalArgumentException if this provider does not know about this lower caching tier
     */
    void releaseCachingTier(LowerCachingTier<?, ?> resource);

    /**
     * Initialises a {@link LowerCachingTier}.
     *
     * @param resource the lower caching tier to initialise
     */
    void initCachingTier(LowerCachingTier<?, ?> resource);
  }

}
