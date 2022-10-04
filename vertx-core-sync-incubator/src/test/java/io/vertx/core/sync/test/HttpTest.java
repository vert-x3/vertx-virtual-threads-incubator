package io.vertx.core.sync.test;

import io.vertx.core.sync.http.HttpClient;
import io.vertx.core.sync.http.HttpClientRequest;
import io.vertx.core.sync.http.HttpClientResponse;
import io.vertx.core.sync.http.HttpServer;
import io.vertx.junit5.VirtualThreadTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpTest extends VirtualThreadTestBase {

  @Test
  public void testFoo() {
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> req.response().end("Hello World"));
    server.listen(8080, "localhost");
    HttpClient client = vertx.createHttpClient();
    HttpClientRequest request = client.request(8080, "localhost", "GET", "/");
    request.end();
    HttpClientResponse response = request.response();
    assertEquals(200, response.statusCode());
    assertEquals("Hello World", response.body().toString());
  }
}
