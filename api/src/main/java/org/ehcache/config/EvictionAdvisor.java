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

package org.ehcache.config;

/**
 * A specialized predicate used to advise on eviction of cache entries.
 * 一种专门的谓词，用于建议删除缓存项。
 *
 * @param <K> the key type for the cache
 * @param <V> the value type for the cache
 */
@FunctionalInterface
public interface EvictionAdvisor<K, V> {

  /**
   * Returns {@code true} if the given key value pair should not be evicted if possible.
   * 如果可能的话不应逐出给定的键值对，则返回{@code true}。
   * <p>
   * Any exception thrown from this method will be logged and the result considered {@code false}.
   * 此方法引发的任何异常都将被记录，结果将被视为{@code false}。
   *
   * @param key the cache key
   * @param value the cache value
   * @return {@code true} if eviction should be avoided, {@code false} otherwise
   */
  boolean adviseAgainstEviction(K key, V value);
}
