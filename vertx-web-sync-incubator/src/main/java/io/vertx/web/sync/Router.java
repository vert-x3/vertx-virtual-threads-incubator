package io.vertx.web.sync;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.sync.http.HttpServerRequest;
import io.vertx.web.sync.impl.RouterImpl;

import static io.vertx.core.http.HttpMethod.*;

public interface Router extends Handler<HttpServerRequest> {

  static Router create() {
    return create(null);
  }

  static Router create(RouterOptions config) {
    return new RouterImpl(config == null ? new RouterOptions() : config);
  }

  Router on(HttpMethod method, String path, WebHandler... handle);

  default Router get(String path, WebHandler handle) {
    return on(GET, path, handle);
  }

  default Router put(String path, WebHandler handle) {
    return on(PUT, path, handle);
  }

  default Router post(String path, WebHandler handle) {
    return on(POST, path, handle);
  }

  default Router delete(String path, WebHandler handle) {
    return on(DELETE, path, handle);
  }

  default Router head(String path, WebHandler handle) {
    return on(HEAD, path, handle);
  }

  default Router patch(String path, WebHandler handle) {
    return on(PATCH, path, handle);
  }

  default Router options(String path, WebHandler handle) {
    return on(OPTIONS, path, handle);
  }

  default Router trace(String path, WebHandler handle) {
    return on(TRACE, path, handle);
  }

  default Router connect(String path, WebHandler handle) {
    return on(CONNECT, path, handle);
  }

  default Router all(String path, WebHandler handle) {
    for (HttpMethod method : HttpMethod.values()) {
      on(method, path, handle);
    }

    return this;
  }}
