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
package org.ehcache.spi.serialization;

import java.nio.ByteBuffer;

/**
 * Defines the contract used to transform type instances to and from a serial form.
 * 定义用于将类型实例转换为串行表单和从串行表单转换为串行表单的约定。
 * <p>
 * Implementations must be thread-safe.
 * 实现必须是线程安全的。
 * <p>
 * When used within the default serialization provider, there is an additional requirement.
 * The implementations must define a constructor that takes in a {@code ClassLoader}.
 * The {@code ClassLoader} value may be {@code null}.  If not {@code null}, the class loader
 * instance provided should be used during deserialization to load classes needed by the deserialized objects.
 * 在默认序列化提供程序中使用时，还有一个附加要求。
 * 这些实现必须定义一个接受{@code ClassLoader}的构造函数。
 * {@code ClassLoader}值可以是{@code null}。如果不是{@code null}，则应在反序列化期间使用提供的类装入器实例来加载反序列化对象所需的类。
 * <p>
 * The serialized object's class must be preserved; deserialization of the serial form of an object must
 * return an object of the same class. The following contract must always be true:
 * 序列化对象的类必须保留；对象串行形式的反序列化必须返回同一类的对象。以下合同必须始终为真：
 * <p>
 * {@code object.getClass().equals( mySerializer.read(mySerializer.serialize(object)).getClass())}
 *
 * @param <T> the type of the instances to serialize
 *
 * @see SerializationProvider
 */
public interface Serializer<T> {

  /**
   * Transforms the given instance into its serial form.
   * 将给定实例转换为其序列形式。
   *
   * @param object the instance to serialize
   *
   * @return the binary representation of the serial form
   *
   * @throws SerializerException if serialization fails
   */
  ByteBuffer serialize(T object) throws SerializerException;

  /**
   * Reconstructs an instance from the given serial form.
   * 从给定的序列形式重建实例。
   *
   * @param binary the binary representation of the serial form
   *
   * @return the de-serialized instance
   *
   * @throws SerializerException if reading the byte buffer fails
   * @throws ClassNotFoundException if the type to de-serialize to cannot be found
   */
  T read(ByteBuffer binary) throws ClassNotFoundException, SerializerException;

  /**
   * Checks if the given instance and serial form {@link Object#equals(Object) represent} the same instance.
   * 检查给定实例和序列形式{@link Object#equals(Object) 是否表示}相同的实例。
   *
   * @param object the instance to check
   * @param binary the serial form to check
   *
   * @return {@code true} if both parameters represent equal instances, {@code false} otherwise
   *
   * @throws SerializerException if reading the byte buffer fails
   * @throws ClassNotFoundException if the type to de-serialize to cannot be found
   */
  boolean equals(T object, ByteBuffer binary) throws ClassNotFoundException, SerializerException;

}
