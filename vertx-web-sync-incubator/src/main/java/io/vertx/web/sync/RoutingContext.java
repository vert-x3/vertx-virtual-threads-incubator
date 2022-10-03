package io.vertx.web.sync;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

public interface RoutingContext {

  String path();

  HttpMethod method();

  String getHeader(CharSequence key);

  RoutingContext putHeader(CharSequence key, CharSequence value);


  WebHandler.HandlerReturn next();

  WebHandler.HandlerReturn end();
  WebHandler.HandlerReturn end(String chunk);
  WebHandler.HandlerReturn end(Buffer chunk);
}
