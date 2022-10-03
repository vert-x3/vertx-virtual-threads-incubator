package io.vertx.web.sync;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

public interface RoutingContext {

  String path();

  HttpMethod method();

  WebHandler.HandlerReturn next();

  WebHandler.HandlerReturn end();
  WebHandler.HandlerReturn end(String chunk);
  WebHandler.HandlerReturn end(Buffer chunk);
}
