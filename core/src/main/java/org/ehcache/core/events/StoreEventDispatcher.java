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

import org.ehcache.core.spi.store.Store;
import org.ehcache.core.spi.store.events.StoreEventSource;

/**
 * Part of the events subsystem at the {@link Store} level.
 * {@link Store}级别的事件子系统的一部分。
 * <p>
 * This interface controls the lifecycle of {@link StoreEventSink}s, enabling implementations to decouple the event
 * raising inside the {@link Store} from the firing to outside collaborators.
 * 此接口控制{@link StoreEventSink}的生命周期，使实现能够将{@link Store}内部引发的事件与外部协作者的触发分离。
 * <p>
 * {@link Store} implementations are expected to get a {@link StoreEventSink} per
 * operation and release it once the operation completes.
 * {@link Store}实现期望每个操作获得一个{@link StoreEventSink}，并在操作完成后释放它。
 */
public interface StoreEventDispatcher<K, V> extends StoreEventSource<K, V> {

  /**
   * Hands over an event sink for recording store events.
   * 移交事件接收器以记录存储区事件。
   *
   * @return the event sink to use
   */
  StoreEventSink<K, V> eventSink();

  /**
   * Releases the event sink after normal completion of an operation.
   * 在操作正常完成后释放事件接收器。
   *
   * @param eventSink the event sink to release
   */
  void releaseEventSink(StoreEventSink<K, V> eventSink);

  /**
   * Releases the event sink after failure of an operation.
   * 在操作失败后释放事件接收器。
   *
   * @param eventSink the event sink to release
   * @param throwable the exception
   */
  void releaseEventSinkAfterFailure(StoreEventSink<K, V> eventSink, Throwable throwable);

  /**
   * Reset an event sink by dropping all queued events.
   * 通过删除所有排队事件重置事件接收器。
   *
   * @param eventSink the event sink to reset
   */
  void reset(StoreEventSink<K, V> eventSink);
}
