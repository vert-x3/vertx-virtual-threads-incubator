package io.vertx.await.impl;

import io.netty.channel.EventLoop;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.*;

import java.util.Objects;
import java.util.concurrent.*;
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

  // Use this instead of a ThreadLocal because must friendly with Virtual Threads!
  // ideally we should use https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/maps/NonBlockingHashMap.java
  // which doesn't use any synchronized op!
  private final ConcurrentHashSet<Thread> inThread = new ConcurrentHashSet<>();

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
      var current = Thread.currentThread();
      inThread.add(current);
      try {
        ctx.dispatch(value, task);
      } finally {
        inThread.remove(current);
      }
    });
  }

  private <T> void execute2(T argument, Handler<T> task) {
    if (Context.isOnWorkerThread()) {
      handle(argument, task);
    } else {
      scheduler.execute(() -> {
        handle(argument, task);
      });
    }
  }

  private <T> void handle(T argument, Handler<T> task) {
    var current = Thread.currentThread();
    inThread.add(current);
    try {
      task.handle(argument);
    } finally {
      inThread.remove(current);
    }
  }

  @Override
  public boolean inThread() {
    return inThread.contains(Thread.currentThread());
  }

  @Override
  public ContextInternal duplicate() {
    // This is fine as we are running on event-loop
    return create(owner(), nettyEventLoop(), scheduler);
  }

  public void lock(Lock lock) {
    var current = Thread.currentThread();
    inThread.remove(current);
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
      inThread.add(current);
    }
  }

  public <T> T await(CompletionStage<T> fut) {
    var current = Thread.currentThread();
    inThread.remove(current);
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
      inThread.add(current);
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
