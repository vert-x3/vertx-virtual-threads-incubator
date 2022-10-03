package io.vertx.web.sync.impl;

import io.vertx.web.sync.RoutingContext;

public interface RoutingContextInternal extends RoutingContext {

  void addParam(String key, String value);
}
