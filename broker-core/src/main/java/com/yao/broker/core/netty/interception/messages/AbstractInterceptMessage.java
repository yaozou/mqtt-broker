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

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * @author yaozou
 */
@SuppressWarnings("AlibabaAbstractClassShouldStartWithAbstractNaming")
public abstract class AbstractInterceptMessage implements InterceptMessage {

  private final MqttMessage msg;

  AbstractInterceptMessage(MqttMessage msg) {
    this.msg = msg;
  }

  public boolean isRetainFlag() {
    return msg.fixedHeader().isRetain();
  }

  public boolean isDupFlag() {
    return msg.fixedHeader().isDup();
  }

  public MqttQoS getQos() {
    return msg.fixedHeader().qosLevel();
  }
}
