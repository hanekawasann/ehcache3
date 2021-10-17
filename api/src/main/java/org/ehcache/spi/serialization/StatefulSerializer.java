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

import org.ehcache.spi.persistence.StateRepository;

/**
 * Implementations of this interface can have their state maintained in a {@code StateRepository}.
 * The state will be maintained by the authoritative tier of the cache for which this is configured.
 * 此接口的实现可以在{@Code StateRepository}中维护其状态。该状态将由为其配置的缓存的权威层维护。
 * <p>
 * Implementations must be thread-safe.
 * 实现必须是线程安全的。
 * <p>
 * When used within the default serialization provider, there is an additional constructor requirement.
 * The implementations must define a constructor that takes in a {@code ClassLoader}.
 * 在默认序列化提供程序中使用时，还有一个额外的构造函数要求。这些实现必须定义一个接受{@code ClassLoader}的构造函数。
 * Post instantiation, the state repository will be injected with the {@code init} method invocation.
 * This is guaranteed to happen before any serialization/deserialization interaction.
 * 实例化后，状态存储库将被注入{@code init}方法调用。这保证在任何序列化-反序列化交互之前发生。
 *
 * @param <T> the type of the instances to serialize
 *
 * @see Serializer
 */
public interface StatefulSerializer<T> extends Serializer<T> {

  /**
   * This method is used to inject a {@code StateRepository} to the serializer
   * by the authoritative tier of a cache during the cache initialization.
   * The passed in state repository will have the persistent properties of the injecting tier.
   * 此方法用于在缓存初始化期间，通过缓存的权威层将{@code StateRepository}注入序列化程序。
   * 传入状态存储库将具有注入层的持久属性。
   *
   * @param stateRepository the state repository
   */
  void init(StateRepository stateRepository);
}
