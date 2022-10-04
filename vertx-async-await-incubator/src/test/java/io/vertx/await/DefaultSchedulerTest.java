package io.vertx.await;

import io.vertx.await.impl.DefaultScheduler;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultSchedulerTest extends SchedulerTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    scheduler = new DefaultScheduler(DefaultScheduler.DEFAULT_THREAD_FACTORY);
  }

  @Test
  public void testSingleThreaded() {
    scheduler.execute(() -> {
      Thread current = Thread.currentThread();
      AtomicBoolean flag = new AtomicBoolean(true);
      scheduler.execute(() -> {
        assertSame(current, Thread.currentThread());
        assertFalse(flag.get());
        testComplete();
      });
      flag.set(false);
    });
    await();
  }
}
