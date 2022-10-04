package io.vertx.await;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EventLoopVirtualThreadContextTest extends VirtualThreadContextTestBase {

  public EventLoopVirtualThreadContextTest() {
    super(true);
  }

  @Test
  public void testSuspend() {
    async.run(v1 -> {
      CompletableFuture<Void> cf = new CompletableFuture<>();
      vertx.runOnContext(v2 -> {
        cf.complete(null);
      });
      try {
        cf.get(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        fail(e);
      }
      testComplete();
    });
    await();
  }

}
