package io.vertx.core.sync.http;


import io.vertx.core.buffer.Buffer;

import static io.vertx.await.Async.await;

public class HttpClientResponse {

  private final io.vertx.core.http.HttpClientResponse delegate;

  public HttpClientResponse(io.vertx.core.http.HttpClientResponse delegate) {
    this.delegate = delegate;
  }

  public int statusCode() {
    return delegate.statusCode();
  }

  public Buffer body() {
    return await(delegate.body());
  }
}
