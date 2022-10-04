package io.vertx.await;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.test.core.VertxTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class HttpTest extends VertxTestBase {

  @Parameterized.Parameters
  public static Collection input() {
    return Arrays.asList(new Object[][] {
      { true },
      { false}
    });
  }

  Async async;
  boolean useVirtualEventLoopThreads;

  public HttpTest(boolean useVirtualEventLoopThreads) {
    this.useVirtualEventLoopThreads = useVirtualEventLoopThreads;
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    async = new Async(vertx, useVirtualEventLoopThreads);
  }

  @Ignore
  @Test
  public void testDuplicate() throws Exception {
    int num = 1000;
    CountDownLatch latch = new CountDownLatch(1);
    async.run(v -> {
      HttpServer server = vertx.createHttpServer(new HttpServerOptions().setInitialSettings(new Http2Settings().setMaxConcurrentStreams(num)));
      CyclicBarrier barrier = new CyclicBarrier(num);
      server.requestHandler(req -> {
        try {
          barrier.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          fail(e);
        } catch (BrokenBarrierException e) {
          fail(e);
        }
        req.response().end("Hello World");
      });
      server.listen(8080, "localhost", onSuccess(v2 -> {
        latch.countDown();
      }));
    });
    awaitLatch(latch);
    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(false)
    );
    waitFor(num);
    for (int i = 0;i < num;i++) {
      client
        .request(HttpMethod.GET, 8080, "localhost", "/")
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(onSuccess(body -> {
          complete();
      }));
    }
    await();
  }

  @Test
  public void testHttpClient1() throws Exception {
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
      req.response().end("Hello World");
    });
    server.listen(8088, "localhost").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    async.run(v -> {
      HttpClient client = vertx.createHttpClient();
      for (int i = 0; i < 100; ++i) {
        HttpClientRequest req = async.await(client.request(HttpMethod.GET, 8088, "localhost", "/"));
        HttpClientResponse resp = async.await(req.send());
        Buffer body = async.await(resp.body());
        String bodyString = body.toString(StandardCharsets.UTF_8);
        assertEquals("Hello World", body.toString());
      }
      testComplete();
    });
    await();
  }

  @Test
  public void testHttpClient2() throws Exception {
    waitFor(100);
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
      req.response().end("Hello World");
    });
    server.listen(8088, "localhost").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    async.run(v -> {
      HttpClient client = vertx.createHttpClient();
      for (int i = 0; i < 100; ++i) {
        HttpClientRequest req = async.await(client.request(HttpMethod.GET, 8088, "localhost", "/"));
        HttpClientResponse resp = async.await(req.send());
        StringBuffer body = new StringBuffer();
        resp.handler(buff -> {
          body.append(buff.toString());
        });
        resp.endHandler(v2 -> {
          assertEquals("Hello World", body.toString());
          complete();
        });
      }
    });
    await();
  }

  @Test
  public void testHttpClientTimeout() throws Exception {
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
    });
    server.listen(8088, "localhost").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    async.run(v -> {
      HttpClient client = vertx.createHttpClient();
      ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
      HttpClientRequest req = async.await(client.request(HttpMethod.GET, 8088, "localhost", "/"));
      PromiseInternal<HttpClientResponse> promise = ctx.promise();
      req.send().onComplete(promise);
      vertx.setTimer(500, id -> promise.tryFail("Too late"));
      try {
        HttpClientResponse resp = async.await(promise.future());
      } catch (Exception e) {
        testComplete();
      }
    });
    await();
  }
}
