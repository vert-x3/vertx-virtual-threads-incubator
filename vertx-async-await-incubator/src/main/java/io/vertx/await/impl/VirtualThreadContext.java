package io.vertx.await.impl;

import io.netty.channel.EventLoop;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.CloseFuture;
import io.vertx.core.impl.ContextBase;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.WorkerPool;
import io.vertx.core.impl.future.FutureInternal;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A fork a WorkerContext with a couple of changes.
 */
public class VirtualThreadContext extends ContextBase {

  public static VirtualThreadContext create(Vertx vertx, EventLoop nettyEventLoop, Scheduler scheduler) {
    VertxImpl _vertx = (VertxImpl) vertx;
    return new VirtualThreadContext(_vertx, nettyEventLoop, _vertx.getInternalWorkerPool(), _vertx.getWorkerPool(), scheduler, null, _vertx.closeFuture(), null);
  }

  private final Scheduler scheduler;
  private Executor executor;

  VirtualThreadContext(VertxInternal vertx,
                       EventLoop eventLoop,
                       WorkerPool internalBlockingPool,
                       WorkerPool workerPool,
                       Scheduler scheduler,
                       Deployment deployment,
                       CloseFuture closeFuture,
                       ClassLoader tccl) {
    super(vertx, eventLoop, internalBlockingPool, workerPool, deployment, closeFuture, tccl);

    this.scheduler = scheduler;
  }

  @Override
  protected void runOnContext(ContextInternal ctx, Handler<Void> action) {
    try {
      run(ctx, null, action);
    } catch (RejectedExecutionException ignore) {
      // Pool is already shut down
    }
  }

  /**
   * <ul>
   *   <li>When the current thread is a worker thread of this context the implementation will execute the {@code task} directly</li>
   *   <li>Otherwise the task will be scheduled on the worker thread for execution</li>
   * </ul>
   */
  @Override
  protected <T> void execute(ContextInternal ctx, T argument, Handler<T> task) {
    execute2(argument, task);
  }

  @Override
  protected <T> void emit(ContextInternal ctx, T argument, Handler<T> task) {
    execute2(argument, arg -> {
      ctx.dispatch(arg, task);
    });
  }

  @Override
  public Executor executor() {
    return scheduler;
  }

  @Override
  protected void execute(ContextInternal ctx, Runnable task) {
    execute(this, task, Runnable::run);
  }

  @Override
  public boolean isEventLoopContext() {
    return false;
  }

  @Override
  public boolean isWorkerContext() {
    return false;
  }

  private <T> void run(ContextInternal ctx, T value, Handler<T> task) {
    Objects.requireNonNull(task, "Task handler must not be null");
    scheduler.execute(() -> {
      ctx.dispatch(value, task);
    });
  }

  private <T> void execute2(T argument, Handler<T> task) {
    if (Context.isOnWorkerThread()) {
      task.handle(argument);
    } else {
      scheduler.execute(() -> {
        task.handle(argument);
      });
    }
  }

  @Override
  public boolean inThread() {
    return scheduler.inThread();
  }

  @Override
  public ContextInternal duplicate() {
    // This is fine as we are running on event-loop
    return create(owner(), nettyEventLoop(), scheduler);
  }

  public <T> T await(FutureInternal<T> future) {
    return scheduler.await(future.toCompletionStage().toCompletableFuture());
  }
}
