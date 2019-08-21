/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package com.yao.broker.core.netty.interception;

import com.yao.broker.core.netty.bean.Subscription;
import com.yao.broker.core.netty.interception.messages.InterceptAcknowledgedMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * This interface is to be used internally by the broker components.
 * <p>
 * An interface is used instead of a class to allow more flexibility in changing an implementation.
 * <p>
 * Interceptor implementations forward notifications to a <code>InterceptHandler</code>, that is
 * normally a field. So, the implementations should act as a proxy to a custom intercept handler.
 *
 * @author yaozou
 * @see InterceptHandler
 */
public interface Interceptor {

  /**
   * notifyClientConnected
   *
   * @param msg MqttConnectMessage
   */
  void notifyClientConnected(MqttConnectMessage msg);

  /**
   * notifyClientDisconnected
   *
   * @param clientID String
   * @param username String
   */
  void notifyClientDisconnected(String clientID, String username);

  /**
   * notifyClientConnectionLost
   *
   * @param clientID String
   * @param username String
   */
  void notifyClientConnectionLost(String clientID, String username);

  /**
   * notifyTopicPublished
   *
   * @param msg      MqttPublishMessage
   * @param clientID String
   * @param username String
   */
  void notifyTopicPublished(MqttPublishMessage msg, String clientID, String username);

  /**
   * notifyTopicSubscribed
   *
   * @param sub      Subscription
   * @param username String
   */
  void notifyTopicSubscribed(Subscription sub, String username);

  /**
   * notifyTopicUnsubscribed
   *
   * @param topic    String
   * @param clientID String
   * @param username String
   */
  void notifyTopicUnsubscribed(String topic, String clientID, String username);

  /**
   * notifyMessageAcknowledged
   *
   * @param msg InterceptAcknowledgedMessage
   */
  void notifyMessageAcknowledged(InterceptAcknowledgedMessage msg);

  /**
   * addInterceptHandler
   *
   * @param interceptHandler InterceptHandler
   */
  void addInterceptHandler(InterceptHandler interceptHandler);

  /**
   * removeInterceptHandler
   *
   * @param interceptHandler InterceptHandler
   */
  void removeInterceptHandler(InterceptHandler interceptHandler);
}
