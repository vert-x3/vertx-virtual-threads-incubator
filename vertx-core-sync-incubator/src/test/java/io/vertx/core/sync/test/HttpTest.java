package io.vertx.core.sync.test;

import io.vertx.core.sync.Vertx;
import io.vertx.core.sync.http.HttpClient;
import io.vertx.core.sync.http.HttpClientRequest;
import io.vertx.core.sync.http.HttpClientResponse;
import io.vertx.core.sync.http.HttpServer;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class HttpTest {

  private Vertx vertx = new Vertx();

  @Before
  public void before() {
    vertx = new Vertx();
  }

  @After
  public void after() {
    try {
      vertx.close();
    } finally {
      vertx = null;
    }
  }

  @Test
  public void testFoo() throws Exception {
    test(() -> {
      HttpServer server = vertx.createHttpServer();
      server.requestHandler(req -> req.response().end("Hello World"));
      server.listen(8080, "localhost");
      HttpClient client = vertx.createHttpClient();
      HttpClientRequest request = client.request(8080, "localhost", "GET", "/");
      request.end();
      HttpClientResponse response = request.response();
      assertEquals(200, response.statusCode());
      assertEquals("Hello World", response.body().toString());
    });
  }

  private void test(Runnable runnable) {
    CompletableFuture<Void> cf = new CompletableFuture<>();
    vertx.execute(v -> {
      try {
        runnable.run();
        cf.complete(null);
      } catch (Throwable t) {
        cf.completeExceptionally(t);
      }
    });
    try {
      cf.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof AssertionFailedError) {
        throw ((AssertionFailedError) cause);
      } else {
        AssertionFailedError afe = new AssertionFailedError();
        afe.initCause(cause);
        throw afe;
      }
    }
  }

}
