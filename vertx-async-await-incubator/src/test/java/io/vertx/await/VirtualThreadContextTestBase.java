package io.vertx.await;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.await.impl.VirtualThreadContext;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.test.core.VertxTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class VirtualThreadContextTestBase extends VertxTestBase {

  Async async;
  boolean useVirtualEventLoopThreads;

  public VirtualThreadContextTestBase(boolean useVirtualEventLoopThreads) {
    this.useVirtualEventLoopThreads = useVirtualEventLoopThreads;
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    async = new Async(vertx, useVirtualEventLoopThreads);
  }

  @Test
  public void testContext() {
    async.run(v -> {
      Thread thread = Thread.currentThread();
      assertTrue(thread.isVirtual());
      Context context = vertx.getOrCreateContext();
      assertTrue(context instanceof VirtualThreadContext);
      context.runOnContext(v2 -> {
        // assertSame(thread, Thread.currentThread());
        assertSame(context, vertx.getOrCreateContext());
        testComplete();
      });
    });
    await();
  }

  @Test
  public void testAwaitFutureSuccess() {
    Object result = new Object();
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      PromiseInternal<Object> promise = context.promise();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }
        promise.complete(result);
      }).start();
      assertSame(result, async.await(promise.future()));
      testComplete();
    });
    await();
  }

  @Test
  public void testAwaitFutureFailure() {
    Object result = new Object();
    Exception failure = new Exception();
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      PromiseInternal<Object> promise = context.promise();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }
        promise.fail(failure);
      }).start();
      try {
        async.await(promise.future());
      } catch (Exception e) {
        assertSame(failure, e);
        testComplete();
        return;
      }
      fail();
    });
    await();
  }

  @Test
  public void testAwaitCompoundFuture() {
    Object result = new Object();
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      PromiseInternal<Object> promise = context.promise();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }
        promise.complete(result);
      }).start();
      assertSame("HELLO", async.await(promise.future().map(res -> "HELLO")));
      testComplete();
    });
    await();
  }

  @Test
  public void testDuplicateUseSameThread() {
    int num = 1000;
    waitFor(num);
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      Thread th = Thread.currentThread();
      for (int i = 0; i < num; i++) {
        ContextInternal duplicate = context.duplicate();
        duplicate.runOnContext(v2 -> {
          // assertSame(th, Thread.currentThread());
          complete();
        });
      }
    });
    await();
  }

  @Test
  public void testDuplicateConcurrentAwait() {
    int num = 1000;
    waitFor(num);
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      Object lock = new Object();
      List<Promise<Void>> list = new ArrayList<>();
      for (int i = 0; i < num; i++) {
        ContextInternal duplicate = context.duplicate();
        duplicate.runOnContext(v2 -> {
          Promise<Void> promise = duplicate.promise();
          boolean complete;
          synchronized (lock) {
            list.add(promise);
            complete = list.size() == num;
          }
          if (complete) {
            context.runOnContext(v3 -> {
              synchronized (lock) {
                list.forEach(p -> p.complete(null));
              }
            });
          }
          Future<Void> f = promise.future();
          async.await(f);
          complete();
        });
      }
    });
    await();
  }

  @Test
  public void testTimer() {
    async.run(v -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      PromiseInternal<String> promise = context.promise();
      vertx.setTimer(100, id -> {
        promise.complete("foo");
      });
      String res = async.await(promise);
      assertEquals("foo", res);
      testComplete();
    });
    await();
  }

  @Test
  public void testInThread() {
    async.run(v1 -> {
      ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
      assertTrue(context.inThread());
      new Thread(() -> {
        boolean wasNotInThread = !context.inThread();
        context.runOnContext(v2 -> {
          assertTrue(wasNotInThread);
          assertTrue(context.inThread());
          testComplete();
        });
      }).start();
    });
    await();
  }

  @Test
  public void testAcquireLock() throws Exception {
    ReentrantLock lock = new ReentrantLock();
    Thread t = new Thread(() -> {
      lock.lock();
      try {
        while (!lock.hasQueuedThreads()) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      } catch (Exception e) {
        fail(e);
      } finally {
        lock.unlock();
      }
    });
    t.start();
    while (!lock.isLocked()) {
      Thread.sleep(10);
    }
    async.run(v1 -> {
      Async.lock(lock);
      assertTrue(lock.isHeldByCurrentThread());
      testComplete();
    });
    await();
  }


  protected void testInterruption(Callable<Void> sleeper, boolean assertIsInterrupted) {
    async.run(v1 -> {
      var thisThread = Thread.currentThread();
      vertx.runOnContext(v2 -> thisThread.interrupt());
      assertFalse(Thread.currentThread().isInterrupted());
      try {
        sleeper.call();
      } catch (InterruptedException e) {
        assertEquals(thisThread, Thread.currentThread());
        assertTrue(Thread.currentThread().isVirtual());
        if (assertIsInterrupted) {
          assertTrue("thread should be interrupted", Thread.currentThread().isInterrupted());
        }
        testComplete();
        return;
      } catch (Exception e) {
        fail(e);
        return;
      }

      fail();
    });

    await();
  }

  @Test
  public void testInterruptsVertxSleepAssertInterrupted() throws Exception {
    testInterruption(() -> {
      sleepVertxInterruptibly(1000);
      return null;
    }, true);
  }

  @Test
  public void testInterruptsThreadSleepAssertInterrupted() throws Exception {
    testInterruption(() -> {
      Thread.sleep(1000);
      return null;
    }, true);
  }

  @Test
  public void testInterruptsVertxSleepIgnoreInterruptedStatus() throws Exception {
    testInterruption(() -> {
      sleepVertxInterruptibly(1000);
      return null;
    }, false);
  }

  @Test
  public void testInterruptsThreadSleepIgnoreInterruptedStatus() throws Exception {
    testInterruption(() -> {
      Thread.sleep(1000);
      return null;
    }, false);
  }

  private void sleepVertxInterruptibly(long milliseconds) throws InterruptedException {
    var promise = Promise.<Long>promise();
    vertx.setTimer(milliseconds, promise::complete);
    try {
      Async.await(promise.future());
    } catch (Throwable t) {
      if (t.getCause() instanceof InterruptedException interruptedException) {
        throw interruptedException;
      }
      throw t;
    }
  }
}
