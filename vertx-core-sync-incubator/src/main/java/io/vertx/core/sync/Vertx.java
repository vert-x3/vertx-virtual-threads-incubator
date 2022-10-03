package io.vertx.core.sync;

import io.vertx.await.Async;
import io.vertx.core.Handler;
import io.vertx.core.sync.http.HttpServer;
import io.vertx.core.sync.http.HttpClient;

public class Vertx {

  private final Async async;
  private final io.vertx.core.Vertx delegate;

  public Vertx() {
    delegate = io.vertx.core.Vertx.vertx();
    async = new Async(delegate);
  }

  public HttpServer createHttpServer() {
    return new HttpServer(delegate);
  }

  public HttpClient createHttpClient() {
    return new HttpClient(delegate.createHttpClient());
  }

  public void execute(Handler<Void> handler) {
    async.run(v -> handler.handle(null));
  }

  public void close() {
    delegate.close();
  }
}
