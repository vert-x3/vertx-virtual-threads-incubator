package io.vertx.core.sync;

import io.vertx.await.Async;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.sync.http.HttpServer;
import io.vertx.core.sync.http.HttpClient;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class Vertx {

  private final Async async;
  private final io.vertx.core.Vertx delegate;

  public Vertx() {
    this(false);
  }

  public Vertx(VertxOptions options) {
    this(new VertxOptions(), false);
  }

  public Vertx(boolean useVirtualEventLoopThreads) {
    this(new VertxOptions(), useVirtualEventLoopThreads);
  }

  /**
   * Build a sync Vert.x instance
   *
   * @param useVirtualEventLoopThreads {@code true} when virtual threads should use the event-loop (requires
   *                                               specific JVM runtime configuration)
   */
  public Vertx(VertxOptions options, boolean useVirtualEventLoopThreads) {
    delegate = io.vertx.core.Vertx.vertx(options);
    async = new Async(delegate, useVirtualEventLoopThreads);
  }

  public io.vertx.core.Vertx unwrap() {
    return delegate;
  }

  public boolean isNativeTransportEnabled() {
    return delegate.isNativeTransportEnabled();
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

  public CompletableFuture<Void> submit(Runnable handler) {
    CompletableFuture<Void> cf = new CompletableFuture<>();
    async.run(v -> {
      try {
        handler.run();
      } catch (Throwable e) {
        cf.completeExceptionally(e);
        return;
      }
      cf.complete(null);
    });
    return cf;
  }

  public <T> CompletableFuture<T> submit(Callable<T> handler) {
    CompletableFuture<T> cf = new CompletableFuture<T>();
    async.run(v -> {
      T res;
      try {
        res = handler.call();
      } catch (Throwable e) {
        cf.completeExceptionally(e);
        return;
      }
      cf.complete(res);
    });
    return cf;
  }

  public void close() {
    delegate.close();
  }
}
