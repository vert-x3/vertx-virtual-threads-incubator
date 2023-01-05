package io.vertx.await;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.executeblocking.ExecuteBlocking;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecuteBlockingTest extends VertxTestBase {

  @Test
  public void testCompletion() {
    vertx.runOnContext(v -> {
      Future<String> fut = ExecuteBlocking.executeBlocking(() -> {
        Thread.sleep(1000);
        return "Hello";
      });
      fut.onComplete(onSuccess(s -> {
        assertEquals("Hello", s);
        testComplete();
      }));
    });
    await();
  }

  @Test
  public void testInterruptsStatusIgnored() {
    testInterrupts(false);
  }

  @Test
  public void testInterruptsStatusAsserted() {
    testInterrupts(true);
  }

  private void testInterrupts(boolean assertInterrupted) {
    vertx.runOnContext(v -> {
      var thisThread = Promise.<Thread>promise();
      var wasInterrupted = ExecuteBlocking.executeBlocking(() -> {
        thisThread.tryComplete(Thread.currentThread());

        try {
          Thread.sleep(10000);
        } catch (InterruptedException interrupted) {
          // we successfully interrupted
          if (assertInterrupted) {
            assertTrue(Thread.currentThread().isInterrupted());
          }
          return null;
        }

        throw new IllegalStateException("should not be reached");
      });

      thisThread.future()
        .compose(thread -> {
          thread.interrupt();
          return wasInterrupted;
        })
        .onComplete(onSuccess(t -> testComplete()));
    });
    await();
  }

  @Test
  public void testFailure() {
    Exception failure = new Exception();
    vertx.runOnContext(v -> {
      Future<String> fut = ExecuteBlocking.executeBlocking(() -> {
        throw failure;
      });
      fut.onComplete(onFailure(err -> {
        assertSame(failure, err);
        testComplete();
      }));
    });
    await();
  }

  @Test
  public void testManyThreads() {
    int num = 100_000;
    vertx.runOnContext(v -> {
      CyclicBarrier barrier = new CyclicBarrier(num);
      AtomicInteger count = new AtomicInteger();
      for (int i = 0; i < num; i++) {
        Future<String> fut = ExecuteBlocking.executeBlocking(() -> {
          barrier.await();
          return "Hello";
        });
        fut.onComplete(onSuccess(s -> {
          if (count.incrementAndGet() == num) {
            testComplete();
          }
        }));
      }
    });
    await();
  }
}
