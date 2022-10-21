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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 200)
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
  @Param({"1", "10", "100"})
  private int stackDepth;

  @Param({"1", "10", "100"})
  private int tasks;

  @Param({"1", "10"})
  private int parks;

  @Param({"0", "1", "10", "100"})
  private int work;

  @State(Scope.Thread)
  public static class InlineExecutor {
    private ArrayDeque<Runnable> tasks;
    protected ThreadFactory threadFactory;
    protected Runnable deepStackFooTask;

    protected int count;

    @Setup
    public void setup(VirtualThreadBenchmark benchmark) {
      tasks = new ArrayDeque<>();
      final ArrayDeque<Runnable> capturedTasks = this.tasks;
      threadFactory = VThreadFactory.createThreadFactory(capturedTasks::addLast);
      count = benchmark.tasks;
      final int depth = benchmark.stackDepth;
      final int work = benchmark.work;
      deepStackFooTask = () -> {
        deepStackFoo(depth, work);
      };
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static void deepStackFoo(int depth, int work) {
      depth--;
      if (depth > 0) {
        deepStackFoo(depth, work);
      } else if (work > 0) {
        Blackhole.consumeCPU(work);
      }
    }

    public void submitTasks() {
      for (int i = 0; i < count; i++) {
        tasks.addLast(deepStackFooTask);
      }
    }

    public void submitVirtualThreads() {
      final ThreadFactory threadFactory = this.threadFactory;
      for (int i = 0; i < count; i++) {
        threadFactory.newThread(deepStackFooTask).start();
      }
    }

    public void executeTasks() {
      final ArrayDeque<Runnable> tasks = this.tasks;
      Runnable task;
      while ((task = tasks.poll()) != null) {
        task.run();
      }
    }
  }

  @State(Scope.Thread)
  public static class ParkingTaskInlineExecutor extends InlineExecutor {
    private Runnable parkTask;
    private Thread[] parkedThreads;

    @Override
    @Setup
    public void setup(VirtualThreadBenchmark benchmark) {
      super.setup(benchmark);
      final int depth = benchmark.stackDepth;
      final int work = benchmark.work;
      final int parks = benchmark.parks;
      parkTask = () -> {
        deepStackPark(depth, work, parks);
      };
      parkedThreads = new Thread[count];
    }

    public void submitParkedVirtualThreads() {
      final ThreadFactory threadFactory = this.threadFactory;
      for (int i = 0; i < count; i++) {
        parkedThreads[i] = threadFactory.newThread(parkTask);
        parkedThreads[i].start();
      }
    }

    public void unpark() {
      for (int i = 0; i < count; i++) {
        LockSupport.unpark(parkedThreads[i]);
        parkedThreads[i] = null;
      }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static void deepStackPark(int depth, int work, int count) {
      depth--;
      if (depth > 0) {
        deepStackPark(depth, work, count);
      } else {
        for (int i = 0; i < count; i++) {
          LockSupport.park();
          if (work > 0) {
            Blackhole.consumeCPU(work);
          }
        }
      }
    }
  }

  /**
   * This benchmark is measuring the cost of creating N virtual threads with a specified stack
   * depth and executing them right after.<br>
   * This is a simulation of using an event loop thread as carrier thread and spawning v threads
   * from within it to be picked and executed by it later.
   */
  @Benchmark
  public void spawnVirtualThreads(InlineExecutor exe) {
    exe.submitVirtualThreads();
    exe.executeTasks();
  }

  /**
   * This benchmark is measuring the cost of creating N parking virtual threads with a specified stack
   * depth, unparking and executing them right after.
   */
  @Benchmark
  public void spawnParkingVirtualThreads(ParkingTaskInlineExecutor exe) {
    exe.submitParkedVirtualThreads();
    // will make them to hit park
    exe.executeTasks();
    // using a different method to help while reading profiling data
    unparkAndResume(exe);
  }

  private void unparkAndResume(ParkingTaskInlineExecutor exe) {
    for (int i = 0; i < parks; i++) {
      exe.unpark();
      exe.executeTasks();
    }
  }

  // baseline
  @Benchmark
  public void spawnRunnables(InlineExecutor exe) {
    exe.submitTasks();
    exe.executeTasks();
  }
}
