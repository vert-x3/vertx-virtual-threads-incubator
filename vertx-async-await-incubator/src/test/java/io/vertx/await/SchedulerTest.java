package io.vertx.await;

import io.vertx.await.impl.Scheduler;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
      scheduler.detach();
    });
    await();
  }

  @Test
  public void testResumeFromAnotherThread() {
    scheduler.execute(() -> {
      CountDownLatch latch = new CountDownLatch(1);
      Consumer<Runnable> detach = scheduler.detach();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        detach.accept(() -> {
          latch.countDown();
        });
        try {
          latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          fail(e);
        }
      }).start();
      testComplete();
    });
    await();
  }

  @Test
  public void testResumeFromContextThread() {
    scheduler.execute(() -> {
      CountDownLatch latch = new CountDownLatch(1);
      Consumer<Runnable> detach = scheduler.detach();
      scheduler.execute(() -> {
        // Make sure the awaiting thread will block on the internal future before resolving it (could use thread status)
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        detach.accept(() -> {
          latch.countDown();
        });
      });
      try {
        latch.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        fail(e);
      }
      testComplete();
    });
    await();
  }
}
