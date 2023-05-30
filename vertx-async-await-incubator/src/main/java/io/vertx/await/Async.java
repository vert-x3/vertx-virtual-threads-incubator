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
  private static final String VTHREAD_CTX = "VTHREAD_CTX";

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
    assert !Thread.currentThread().isVirtual();
    Context ctx = vertx.getOrCreateContext();
    if (!ctx.isEventLoopContext()) {
      throw new IllegalStateException();
    }
    var unsafeCtx = (ContextInternal) ctx;
    VirtualThreadContext virtualCtx = unsafeCtx.getLocal(VTHREAD_CTX);
    if (virtualCtx == null) {
      EventLoop eventLoop = unsafeCtx.nettyEventLoop();
      // Scheduler scheduler = useVirtualEventLoopThreads ? new SchedulerImpl(LoomaniaScheduler2.threadFactory(eventLoop)): new SchedulerImpl(SchedulerImpl.DEFAULT_THREAD_FACTORY);
      Scheduler scheduler = useVirtualEventLoopThreads ? new EventLoopScheduler(eventLoop) : new DefaultScheduler(DefaultScheduler.DEFAULT_THREAD_FACTORY);
      virtualCtx = VirtualThreadContext.create(vertx, eventLoop, scheduler);
      unsafeCtx.putLocal(VTHREAD_CTX, virtualCtx);
    }
    virtualCtx.runOnContext(task);
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
