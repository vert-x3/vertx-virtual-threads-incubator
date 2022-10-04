package io.vertx.await;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.vertx.await.impl.EventLoopScheduler;

public class EventLoopSchedulerTest extends SchedulerTestBase {

  private EventLoopGroup group;
  private EventLoop eventLoop;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    group = new NioEventLoopGroup(1);
    eventLoop = group.next();
    scheduler = new EventLoopScheduler(eventLoop);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    eventLoop.close();
    group.close();
  }
}
