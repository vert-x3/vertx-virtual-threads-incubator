package io.vertx.core.sync;

import io.vertx.await.Async;
import io.vertx.core.Handler;
import io.vertx.core.sync.http.HttpServer;
import io.vertx.core.sync.http.HttpClient;

public class Vertx {

  private final Async async;
  private final io.vertx.core.Vertx delegate;

  public Vertx() {
    this(false);
  }

  /**
   * Build a sync Vert.x instance
   *
   * @param useVirtualEventLoopThreads {@code true} when virtual threads should use the event-loop (requires
   *                                               specific JVM runtime configuration)
   */
  public Vertx(boolean useVirtualEventLoopThreads) {
    delegate = io.vertx.core.Vertx.vertx();
    async = new Async(delegate, useVirtualEventLoopThreads);
  }

  public io.vertx.core.Vertx unwrap() {
    return delegate;
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
