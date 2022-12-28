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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * A fork a WorkerContext with a couple of changes.
 */
public class VirtualThreadContext extends ContextBase {

  public static VirtualThreadContext create(Vertx vertx, EventLoop nettyEventLoop, Scheduler scheduler) {
    VertxImpl _vertx = (VertxImpl) vertx;
    return new VirtualThreadContext(_vertx, nettyEventLoop, _vertx.getInternalWorkerPool(), _vertx.getWorkerPool(), scheduler, null, _vertx.closeFuture(), null);
  }

  private final Scheduler scheduler;
  private final ThreadLocal<Boolean> inThread = new ThreadLocal<>();

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
      inThread.set(true);
      try {
        ctx.dispatch(value, task);
      } finally {
        inThread.remove();
      }
    });
  }

  private <T> void execute2(T argument, Handler<T> task) {
    if (Context.isOnWorkerThread()) {
      inThread.set(true);
      try {
        task.handle(argument);
      } finally {
        inThread.remove();
      }
    } else {
      scheduler.execute(() -> {
        inThread.set(true);
        try {
          task.handle(argument);
        } finally {
          inThread.remove();
        }
      });
    }
  }

  @Override
  public boolean inThread() {
    return inThread.get() == Boolean.TRUE;
  }

  @Override
  public ContextInternal duplicate() {
    // This is fine as we are running on event-loop
    return create(owner(), nettyEventLoop(), scheduler);
  }

  public void lock(Lock lock) {
    inThread.remove();
    Consumer<Runnable> cont = scheduler.unschedule();
    CompletableFuture<Void> latch = new CompletableFuture<>();
    try {
      lock.lock();
      cont.accept(() -> latch.complete(null));
    } catch(RuntimeException e) {
      cont.accept(() -> latch.completeExceptionally(e));
    }
    try {
      latch.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throwAsUnchecked(e);
    } finally {
      inThread.set(true);
    }
  }

  public <T> T await(CompletableFuture<T> fut) {
    if (fut.state() == java.util.concurrent.Future.State.SUCCESS) {
      return fut.resultNow();
    }
    if (fut.state() == java.util.concurrent.Future.State.FAILED) {
      throwAsUnchecked(fut.exceptionNow());
      return null;
    }
    inThread.remove();
    Consumer<Runnable> cont = scheduler.unschedule();
    CompletableFuture<T> latch = new CompletableFuture<>();
    fut.whenComplete((v, err) -> {
      cont.accept(() -> {
        doComplete(v, err, latch);
      });
    });
    try {
      return latch.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throwAsUnchecked(e.getCause());
      return null;
    } finally {
      inThread.set(true);
    }
  }

  private static <T> void doComplete(T val, Throwable err, CompletableFuture<T> fut) {
    if (err == null) {
      fut.complete(val);
    } else {
      fut.completeExceptionally(err);
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void throwAsUnchecked(Throwable t) throws E {
    throw (E) t;
  }
}
