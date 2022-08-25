package io.vertx.await;

import io.vertx.core.Future;
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
      for (int i = 0;i < num;i++) {
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
