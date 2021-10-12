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

package org.ehcache.core.spi.store;

import org.ehcache.spi.service.PluralService;
import org.ehcache.spi.service.ServiceConfiguration;

import java.util.Collection;

/**
 * Marker interface for {@link Store}s which act like wrapper and does not have any storage, rather
 * delegate the storage to other stores
 * {@link Store}的标记接口，其作用类似于包装器，没有任何存储，而是将存储委托给其他存储
 * @param <K> the key type
 * @param <V> the value type
 */
public interface WrapperStore<K, V> extends Store<K, V> {

  /**
   * Service to create {@link WrapperStore}s
   */
  @PluralService
  interface Provider extends Store.Provider {

    /**
     * Gets the internal ranking for the {@code WrapperStore} instances provided by this {@code Provider} of the wrapper
     * store's
     * 获取包装存储的{@code Provider}提供的{@code WrapperStore}实例的内部排名
     *
     * @param serviceConfigs the collection of {@code ServiceConfiguration} instances that may contribute
     *                       to the ranking
     * @return a non-negative rank indicating the ability of a {@code WrapperStore} created by this {@code Provider}
     */
    int wrapperStoreRank(Collection<ServiceConfiguration<?, ?>> serviceConfigs);

  }
}
