package io.vertx.await;

import io.netty.channel.EventLoop;
import io.vertx.await.impl.EventLoopScheduler;
import io.vertx.await.impl.Scheduler;
import io.vertx.await.impl.VirtualThreadContext;
import io.vertx.await.impl.DefaultScheduler;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;

public class Async {

  private final Vertx vertx;
  private final boolean useVirtualEventLoopThreads;

  public Async(Vertx vertx) {
    this(vertx, false);
  }

  public Async(Vertx vertx, boolean useVirtualEventLoopThreads) {
    this.vertx = vertx;
    this.useVirtualEventLoopThreads = useVirtualEventLoopThreads;
  }

  /**
   * Run a task on a virtual thread
   */
  public void run(Handler<Void> task) {
    Context ctx = vertx.getOrCreateContext();
    EventLoop eventLoop;
    if (ctx.isEventLoopContext()) {
      eventLoop = ((ContextInternal)ctx).nettyEventLoop();
    } else {
      throw new IllegalStateException();
    }
    // Scheduler scheduler = useVirtualEventLoopThreads ? new SchedulerImpl(LoomaniaScheduler2.threadFactory(eventLoop)): new SchedulerImpl(SchedulerImpl.DEFAULT_THREAD_FACTORY);
    Scheduler scheduler = useVirtualEventLoopThreads ? new EventLoopScheduler(eventLoop) : new DefaultScheduler(DefaultScheduler.DEFAULT_THREAD_FACTORY);
    VirtualThreadContext context = VirtualThreadContext.create(vertx, eventLoop, scheduler);
    context.runOnContext(task);
  }

  private static VirtualThreadContext virtualThreadContext() {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    if (ctx != null) {
      ctx = ctx.unwrap();
      if (ctx instanceof VirtualThreadContext) {
        return ((VirtualThreadContext)ctx);
      }
    }
    throw new IllegalStateException("Not running on a Vert.x virtual thread");
  }

  public static <T> T await(Future<T> future) {
    return await(future.toCompletionStage().toCompletableFuture());
  }

  public static void lock(Lock lock) {
    VirtualThreadContext ctx = virtualThreadContext();
    ctx.lock(lock);
  }

  public static <T> T await(CompletionStage<T> future) {
    VirtualThreadContext ctx = virtualThreadContext();
    return ctx.await(future);
  }
}
