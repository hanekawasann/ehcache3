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
package org.ehcache.spi.resilience;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;

/**
 * A {@link Service} that creates {@link ResilienceStrategy} instances.
 * 创建{@link ResilienceStrategy}实例的{@link Service}。
 * <p>
 * A {@code CacheManager} will use the {@link #createResilienceStrategy(String, CacheConfiguration, RecoveryStore)} and
 * {@link #createResilienceStrategy(String, CacheConfiguration, RecoveryStore, CacheLoaderWriter)} methods to create
 * {@code ResilienceStrategy} instances for each {@code Cache} it manages.
 * {@code CacheManager}将使用{@link createResilienceStrategy（String，CacheConfiguration，RecoveryStore）}和{@link createResilienceStrategy（String，CacheConfiguration，RecoveryStore，CacheLoaderWriter）}方法为其管理的每个{@code Cache}创建{@code ResilienceStrategy}实例。
 */
public interface ResilienceStrategyProvider extends Service {

  /**
   * Creates a {@code ResilienceStrategy} for the {@link org.ehcache.Cache Cache} with the given alias and configuration
   * using the given {@link RecoveryStore}.
   * 使用给定的{@link RecoveryStore}为具有给定别名和配置的{@link org.ehcache.Cache Cache}创建{@code ResilienceStrategy}。
   *
   * @param alias the {@code Cache} alias in the {@code CacheManager}
   * @param configuration the configuration for the associated cache
   * @param recoveryStore the associated recovery store
   * @param <K> the stores key type
   * @param <V> the stores value type
   * @return the {@code ResilienceStrategy} to be used by the {@code Cache}
   */
  <K, V> ResilienceStrategy<K, V> createResilienceStrategy(String alias, CacheConfiguration<K, V> configuration,
                                                           RecoveryStore<K> recoveryStore);

  /**
   * Creates a {@code ResilienceStrategy} for the {@link org.ehcache.Cache Cache} with the given alias and configuration
   * using the given {@link RecoveryStore} and {@link CacheLoaderWriter}
   * 使用给定的{@link RecoveryStore}和{@link CacheLoaderWriter}为具有给定别名和配置的{@link org.ehcache.Cache Cache}创建{@code ResilienceStrategy}
   *
   * @param alias the {@code Cache} alias in the {@code CacheManager}
   * @param configuration the configuration for the associated cache
   * @param recoveryStore the associated recovery store
   * @param loaderWriter the associated loader-writer
   * @param <K> the stores key type
   * @param <V> the stores value type
   * @return the {@code ResilienceStrategy} to be used by the {@code Cache}
   */
  <K, V> ResilienceStrategy<K, V> createResilienceStrategy(String alias, CacheConfiguration<K, V> configuration,
                                                           RecoveryStore<K> recoveryStore, CacheLoaderWriter<? super K, V> loaderWriter);
}
