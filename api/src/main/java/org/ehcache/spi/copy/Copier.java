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

package org.ehcache.spi.copy;

/**
 * Defines the contract used to copy type instances.
 * 定义用于复制类型实例的约定。
 * <p>
 * The copied object's class must be preserved.  The following must always be true:
 * 必须保留复制对象的类。以下内容必须始终正确：
 * <p>
 * <code>object.getClass().equals( myCopier.copyForRead(object).getClass() )</code>
 * <code>object.getClass().equals( myCopier.copyForWrite(object).getClass() )</code>
 *
 * @param <T> the type of the instance to copy
 */
public interface Copier<T> {

  /**
   * Creates a copy of the instance passed in.
   * <p>
   * This method is invoked as a value is read from the cache.
   * 创建传入实例的副本<p> 从缓存中读取值时调用此方法。
   *
   * @param obj the instance to copy
   * @return the copy of the {@code obj} instance
   */
  T copyForRead(T obj);

  /**
   * Creates a copy of the instance passed in.
   * <p>
   * This method is invoked as a value is written to the cache.
   * 创建传入实例的副本<p> 将值写入缓存时调用此方法。
   *
   * @param obj the instance to copy
   * @return the copy of the {@code obj} instance
   */
  T copyForWrite(T obj);
}
