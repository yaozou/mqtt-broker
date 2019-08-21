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

package com.yao.broker.core.netty.interception.messages;

import com.yao.broker.core.netty.bean.Subscription;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * @author yaozou
 */
public class InterceptSubscribeMessage implements InterceptMessage {

  private final Subscription subscription;
  private final String username;

  public InterceptSubscribeMessage(Subscription subscription, String username) {
    this.subscription = subscription;
    this.username = username;
  }

  public String getClientID() {
    return subscription.getClientId();
  }

  public MqttQoS getRequestedQos() {
    return subscription.getQos();
  }

  public String getTopicFilter() {
    return subscription.getTopic().toString();
  }

  public String getUsername() {
    return username;
  }
}
