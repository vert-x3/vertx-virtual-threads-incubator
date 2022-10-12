package io.vertx.await.impl;

import io.netty.channel.EventLoop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public class EventLoopScheduler implements Scheduler {

  static final boolean ok;
  static final MethodHandle virtualThreadFactory;

  public static boolean isAvailable() {
    return ok;
  }

  static {
    boolean isOk = false;
    MethodHandle vtf = null;
    try {
      MethodHandles.Lookup thr = MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());
      Class<?> vtbClass = Class.forName("java.lang.ThreadBuilders$VirtualThreadBuilder", false, null);
      vtf = thr.findConstructor(vtbClass, MethodType.methodType(void.class, Executor.class));
      // create efficient transformer
      vtf = vtf.asType(MethodType.methodType(Thread.Builder.OfVirtual.class, Executor.class));
      isOk = true;
    } catch (Exception | Error e) {
      // no good
      System.err.println("Failed to initialize: " + e);
    }
    ok = isOk;
    virtualThreadFactory = vtf;
  }

  private static ThreadFactory threadFactory(Executor carrier) {
    try {
      Thread.Builder.OfVirtual ov = (Thread.Builder.OfVirtual) virtualThreadFactory.invokeExact(carrier);
      ov.name("vert.x-virtual-thread");
      return ov.factory();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  private final ThreadFactory threadFactory;
  private final LinkedList<Runnable> tasks = new LinkedList<>();
  private boolean flag;

  public EventLoopScheduler(EventLoop carrier) {
    this(command -> {
      if (carrier.inEventLoop()) {
        command.run();
      } else {
        carrier.execute(command);
      }
    });
  }

  public EventLoopScheduler(Executor carrier) {
    threadFactory = threadFactory(command -> {
      if (flag) {
        tasks.addLast(command);
      } else {
        tasks.addFirst(command);
      }
      carrier.execute(() -> {
        tasks.poll().run();
      });
    });
  }

  @Override
  public Consumer<Runnable> unschedule() {
    return Runnable::run;
  }

  public void execute(Runnable runnable) {
    Thread thread = threadFactory.newThread(runnable);
    flag = true;
    try {
      thread.start();
    } finally {
      flag = false;
    }
  }
}
