package io.vertx.core.sync.http;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

public class HttpServerResponse {

  private io.vertx.core.http.HttpServerResponse delegate;

  HttpServerResponse(io.vertx.core.http.HttpServerResponse delegate) {
    this.delegate = delegate;
  }

  public HttpServerResponse status(int sc) {
    delegate.setStatusCode(sc);
    return this;
  }

  public MultiMap headers() {
    return delegate.headers();
  }

  public void end(Buffer chunk) {
    delegate.end(chunk);
  }

  public void end(String chunk) {
    delegate.end(chunk);
  }

  public void end() {
    delegate.end();
  }

}
