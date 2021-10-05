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

package org.ehcache.spi.service;

/**
 * A life-cycled service that supports cache functionality.
 * <p>
 * Implementation of this interface must be thread-safe.
 * <p>
 * Since {@code CacheManager}s can be closed and initialized again, {@code Service} implementations should support
 * multiple start/stop cycles. Failure to do so will limit the init/close cycles at the {@code CacheManager} level.
 */
public interface Service {

  /**
   * Start this service using the provided configuration and {@link ServiceProvider}.
   * 使用提供的配置和{@link ServiceProvider}启动此服务。
   * <p>
   * The service provider allows a service to retrieve and use other services.
   * 服务提供者允许服务检索和使用其他服务。
   * <p>
   * A {@code Service} retrieved at this stage may not yet be started. The recommended usage pattern therefore, is to keep a
   * reference to the dependent {@code Service} but use it only when specific methods are invoked on subtypes.
   * 在此阶段检索到的{@code Service}可能尚未启动。因此，建议的使用模式是保留对依赖{@code Service}的引用，但仅在对子类型调用特定方法时使用它。
   *
   * @param serviceProvider the service provider
   */
  void start(ServiceProvider<Service> serviceProvider);

  /**
   * Stops this service.
   */
  void stop();
}
