package io.vertx.web.sync;

import io.vertx.core.sync.Vertx;
import io.vertx.core.sync.http.HttpClient;
import io.vertx.core.sync.http.HttpClientRequest;
import io.vertx.core.sync.http.HttpClientResponse;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class RouterTest {

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

  @Test
  public void testBootstrapWeb() throws Exception {

    test(() -> {
      final Router app = Router.create();

      app.get(
        "/",
        ctx -> {
          System.out.println("Logging request! " + ctx);
          return ctx.next();
        },
        ctx -> {
          return ctx.end("Hello World");
        }
      );

      vertx.createHttpServer()
        .requestHandler(app)
        .listen(8080, "0.0.0.0");


      HttpClient client = vertx.createHttpClient();
      HttpClientRequest request = client.request(8080, "localhost", "GET", "/");
      request.end();
      HttpClientResponse response = request.response();
      assertEquals(200, response.statusCode());
      assertEquals("Hello World", response.body().toString());
    });

  }
}
