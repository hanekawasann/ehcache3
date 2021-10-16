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

package org.ehcache.core.spi.store.events;

import org.ehcache.core.events.StoreEventDispatcher;

/**
 * Interface used to register on a {@link StoreEventSource} to get notified of events happening to mappings the
 * {@link org.ehcache.core.spi.store.Store} contains.
 * 接口，用于在{@link StoreEventSource}上注册以获得{@link org.ehcache.core.spi.store.Store}包含的映射发生事件的通知。
 * <p>
 * Implementations of this class are expected to work in combination with an implementation of
 * {@link StoreEventDispatcher}.
 * 此类的实现应与{@link StoreEventDispatcher}的实现结合使用。
 *
 * @param <K> the key type of the mappings
 * @param <V> the value type of the mappings
 */
public interface StoreEventListener<K, V> {

  /**
   * Invoked on any {@link StoreEvent}.
   *
   * @param event the actual {@link StoreEvent}
   */
  void onEvent(StoreEvent<K, V> event);
}
