package io.vertx.await;

import io.vertx.await.impl.Scheduler;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SchedulerTest extends VertxTestBase {


  private static void sleep() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private Scheduler scheduler;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    scheduler = new Scheduler();
    disableThreadChecks();
  }

  @Test
  public void testSingleThreaded() {
    scheduler.execute(() -> {
      Thread current = Thread.currentThread();
      scheduler.execute(() -> {
        assertSame(current, Thread.currentThread());
        testComplete();
      });
    });
    await();
  }

  @Test
  public void testCreateThread() throws Exception {
    AtomicReference<Thread> thread = new AtomicReference<>();
    scheduler.execute(() -> {
      thread.set(Thread.currentThread());
    });
    waitUntil(() -> thread.get() != null);
    Thread.sleep(10);
    scheduler.execute(() -> {
      assertNotSame(thread.get(), Thread.currentThread());
      testComplete();
    });
    await();
  }

  @Test
  public void testAwaitSchedulesOnNewThread() {
    scheduler.execute(() -> {
      Thread current = Thread.currentThread();
      scheduler.execute(() -> {
        assertNotSame(current, Thread.currentThread());
        testComplete();
      });
      CompletableFuture<Void> cf = new CompletableFuture<>();
      scheduler.await(cf);
    });
    await();
  }

  @Test
  public void testResumeFromAnotherThread() {
    Object expected = new Object();
    scheduler.execute(() -> {
      CompletableFuture<Object> cf = new CompletableFuture<>();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        cf.complete(expected);
      }).start();
      Object res = scheduler.await(cf);
      assertSame(expected, res);
      testComplete();
    });
    await();
  }

  @Test
  public void testResumeFromContextThread() {
    Object expected = new Object();
    scheduler.execute(() -> {
      CompletableFuture<Object> cf = new CompletableFuture<>();
      scheduler.execute(() -> {
        // Make sure the awaiting thread will block on the internal future before resolving it (could use thread status)
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        cf.complete(expected);
      });
      Object res = scheduler.await(cf);
      assertSame(expected, res);
      testComplete();
    });
    await();
  }
}
