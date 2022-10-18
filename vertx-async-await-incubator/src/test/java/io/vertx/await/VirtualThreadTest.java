package io.vertx.await;

import io.vertx.benchmarks.VirtualThreadBenchmark;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

public class VirtualThreadTest {

  @Test
  public void theTest() {

    ArrayDeque<Runnable> commands = new ArrayDeque<>();

    ThreadFactory factory = VirtualThreadBenchmark.threadFactory(new Executor() {
      @Override
      public void execute(Runnable command) {
        commands.add(command);
      }
    });

    Thread th = factory.newThread(() -> {
      System.out.println("a");
      LockSupport.park();
      System.out.println("b");
    });
    th.start();

    commands.poll().run();
    LockSupport.unpark(th);
    commands.poll().run();


  }
}
