package io.vertx.web.sync;

import io.vertx.core.sync.http.HttpClient;
import io.vertx.core.sync.http.HttpClientRequest;
import io.vertx.core.sync.http.HttpClientResponse;
import io.vertx.junit5.VirtualThreadTestBase;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.web.sync.handler.BasicAuthHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouterTest extends VirtualThreadTestBase {

  @Test
  public void testBootstrapWeb() {
    final Router app = Router.create();

    app.get(
      "/",
      ctx -> {
        System.out.println("Logging request! " + ctx);
        return ctx.next();
      },
      new BasicAuthHandler(PropertyFileAuthentication.create(vertx.unwrap(), "test-auth.properties")),
      ctx -> {
        return ctx.end("Hello World");
      }
    );

    vertx.createHttpServer()
      .requestHandler(app)
      .listen(8080, "0.0.0.0");


    HttpClient client = vertx.createHttpClient();
    HttpClientRequest request = client.request(8080, "localhost", "GET", "/");
    request.putHeader("AUTHORIZATION", "Basic cGF1bG86c2VjcmV0");
    request.end();
    HttpClientResponse response = request.response();
    assertEquals(200, response.statusCode());
    assertEquals("Hello World", response.body().toString());
  }
}
