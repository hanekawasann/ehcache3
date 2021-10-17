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

package org.ehcache.core.events;

import java.util.function.Supplier;

/**
 * Interface on which {@link org.ehcache.core.spi.store.Store} operations are to record events.
 * 用于记录事件的{@link org.ehcache.core.spi.store.Store}操作的接口。
 */
public interface StoreEventSink<K, V> {

  /**
   * Indicates the mapping was removed.
   * 指示已删除映射。
   *
   * @param key removed key
   * @param value value supplier of removed value
   */
  void removed(K key, Supplier<V> value);

  /**
   * Indicates the mapping was updated.
   * 指示映射已更新。
   *
   * @param key the updated key
   * @param oldValue value supplier of old value
   * @param newValue the new value
   */
  void updated(K key, Supplier<V> oldValue, V newValue);

  /**
   * Indicates the mapping was expired.
   * 指示映射已过期。
   *
   * @param key the expired key
   * @param value value supplier of expired value
   */
  void expired(K key, Supplier<V> value);

  /**
   * Indicates a mapping was created.
   * 指示已创建映射。
   *
   * @param key the created key
   * @param value the created value
   */
  void created(K key, V value);

  /**
   * Indicates a mapping was evicted.
   * 指示已逐出映射。
   *
   * @param key the evicted key
   * @param value value supplier of evicted value
   */
  void evicted(K key, Supplier<V> value);
}
