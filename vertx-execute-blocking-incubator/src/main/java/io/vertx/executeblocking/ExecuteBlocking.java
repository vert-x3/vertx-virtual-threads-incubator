package io.vertx.executeblocking;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.future.PromiseInternal;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

/**
 * Virtual thread execute blocking construct.
 */
public class ExecuteBlocking {

  private static final ThreadFactory threadFactory = Thread.ofVirtual().name("vert.x-virtual-thread-worker-", 0).factory();

  public static <T> Future<T> executeBlocking(Callable<T> task) {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    PromiseInternal<T> promise = ctx.promise();
    Thread thread = threadFactory.newThread(() -> {
      T t;
      try {
        t = task.call();
      } catch (Exception e) {
        promise.fail(e);
        return;
      }
      promise.complete(t);
    });
    thread.start();
    return promise.future();
  }
}
