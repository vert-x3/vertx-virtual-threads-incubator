package io.vertx.core.sync.http;

import io.vertx.core.buffer.Buffer;

import static io.vertx.await.Async.await;

public class HttpServerRequest {

  private final io.vertx.core.http.HttpServerRequest delegate;
  private final HttpServerResponse response;

  HttpServerRequest(io.vertx.core.http.HttpServerRequest delegate) {
    this.delegate = delegate;
    this.response = new HttpServerResponse(delegate.response());
  }

  public String method() {
    return delegate.method().name();
  }

  public Buffer body() {
    return await(delegate.body());
  }

  public HttpServerResponse response() {
    return response;
  }
}
