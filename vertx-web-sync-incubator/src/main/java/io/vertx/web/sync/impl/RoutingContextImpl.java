package io.vertx.web.sync.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.sync.http.HttpServerRequest;
import io.vertx.core.sync.http.HttpServerResponse;
import io.vertx.web.sync.RoutingContext;
import io.vertx.web.sync.WebHandler;

public class RoutingContextImpl implements RoutingContextInternal {

  private final HttpServerRequest req;
  private final HttpServerResponse res;

  private MultiMap params;

  RoutingContextImpl(HttpServerRequest req) {
    this.req = req;
    this.res = req.response();
  }

  @Override
  public String path() {
    return req.path();
  }

  @Override
  public HttpMethod method() {
    return req.method();
  }

  @Override
  public String getHeader(CharSequence key) {
    return req.headers().get(key);
  }

  public RoutingContext putHeader(CharSequence key, CharSequence value) {
    res.headers().add(key, value);
    return this;
  }

  @Override
  public WebHandler.HandlerReturn next() {
    return WebHandler.HandlerReturn.NEXT;
  }

  @Override
  public WebHandler.HandlerReturn end() {
    res.end();
    return WebHandler.HandlerReturn.END;
  }

  @Override
  public WebHandler.HandlerReturn end(String chunk) {
    res.end(chunk);
    return WebHandler.HandlerReturn.END;
  }

  @Override
  public WebHandler.HandlerReturn end(Buffer chunk) {
    res.end(chunk);
    return WebHandler.HandlerReturn.END;
  }

  @Override
  public void addParam(String key, String value) {
    if (params == null) {
      params = MultiMap.caseInsensitiveMultiMap();
    }

    params.add(key, value);
  }
}
