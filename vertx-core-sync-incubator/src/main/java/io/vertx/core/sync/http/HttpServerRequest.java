package io.vertx.core.sync.http;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

import static io.vertx.await.Async.await;

public class HttpServerRequest {

  private final io.vertx.core.http.HttpServerRequest delegate;
  private final HttpServerResponse response;

  HttpServerRequest(io.vertx.core.http.HttpServerRequest delegate) {
    this.delegate = delegate;
    this.response = new HttpServerResponse(delegate.response());
  }

  public String getParam(String name) {
    return delegate.getParam(name);
  }

  public HttpMethod method() {
    return delegate.method();
  }

  public MultiMap headers() {
    return delegate.headers();
  }

  public String path() {
    return delegate.path();
  }

  public Buffer body() {
    return await(delegate.body());
  }

  public HttpServerResponse response() {
    return response;
  }

}
