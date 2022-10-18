/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 5, time = 3)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = {
  "--enable-preview",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.vm=ALL-UNNAMED"
})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class VirtualThreadBenchmark {

  private  static final boolean ok;
  private static final MethodHandle virtualThreadFactory;

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

  public static ThreadFactory threadFactory(Executor carrier) {
    try {
      Thread.Builder.OfVirtual ov = (Thread.Builder.OfVirtual) virtualThreadFactory.invokeExact(carrier);
      return ov.factory();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @org.openjdk.jmh.annotations.State(Scope.Thread)
  public static class State1 {

    ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    ThreadFactory threadFactory;
    Runnable th;

    @Setup
    public void setup(Blackhole bh) {
      threadFactory = threadFactory(task -> {
        tasks.addLast(task);
      });
      th = () -> bh.consume(0);
    }
  }

  @Benchmark
  public void execute1(State1 state) {
    Thread thread = state.threadFactory.newThread(state.th);
    thread.start();
    Runnable task;
    while ((task = state.tasks.poll()) != null) {
      task.run();
    }
  }

  @org.openjdk.jmh.annotations.State(Scope.Thread)
  public static class State2 {

    ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    Thread thread;

    @Setup
    public void setup(Blackhole bh) {
      ThreadFactory threadFactory = threadFactory(task -> {
        tasks.addLast(task);
      });
      thread = threadFactory.newThread(() -> {
        while (true) {
          LockSupport.park();
          bh.consume(0);
        }
      });
      thread.start();
      tasks.poll().run(); // park the virtual thread
    }
  }

  @Benchmark
  public void execute2(State2 state) {
    LockSupport.unpark(state.thread);
    Runnable task;
    while ((task = state.tasks.poll()) != null) {
      task.run();
    }
  }

  // baseline
  @Benchmark
  public void execute3(Blackhole bh) {
    bh.consume(0);
  }
}
