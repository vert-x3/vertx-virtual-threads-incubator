package io.vertx.await;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.test.core.VertxTestBase;
import org.junit.Before;
import org.junit.Test;

import static io.vertx.await.Async.await;

public class EventBusTest extends VertxTestBase {

  Async async;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    async = new Async(vertx);
  }

  @Test
  public void testEventBus() throws Exception {
    EventBus eb = vertx.eventBus();
    eb.consumer("test-addr", msg -> {
      msg.reply(msg.body());
    });
    async.run(v -> {
      Message<String> ret = Async.await(eb.request("test-addr", "test"));
      assertEquals("test", ret.body());
      testComplete();
    });
    await();
  }
}
