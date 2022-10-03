package io.vertx.core.sync.http;

import io.vertx.core.buffer.Buffer;
import static io.vertx.await.Async.await;

public class HttpClientRequest {

  private final io.vertx.core.http.HttpClientRequest delegate;

  public HttpClientRequest(io.vertx.core.http.HttpClientRequest delegate) {
    this.delegate = delegate;
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

  public HttpClientResponse response() {
    return new HttpClientResponse(await(delegate.response()));
  }
}
