package io.vertx.core.sync.http;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;

import static io.vertx.await.Async.await;

public class HttpServer {

  private final io.vertx.core.Vertx vertx;
  private Handler<HttpServerRequest> requestHandler;
  private io.vertx.core.http.HttpServer delegate;

  public HttpServer(Vertx vertx) {
    this.vertx = vertx;
  }

  public synchronized HttpServer requestHandler(Handler<HttpServerRequest> requestHandler) {
    this.requestHandler = requestHandler;
    return this;
  }

  public void listen(int port, String host) {
    synchronized (this) {
      if (delegate != null) {
        throw new IllegalStateException();
      }
      Handler<HttpServerRequest> handler = requestHandler;
      if (handler == null) {
        throw new IllegalStateException();
      }
      delegate = vertx.createHttpServer().requestHandler(req -> {
        requestHandler.handle(new HttpServerRequest(req));
      });
    }
    delegate.requestHandler();
    await(delegate.listen(port, host));
  }

  public void close() {
    await(delegate.close());
  }
}
