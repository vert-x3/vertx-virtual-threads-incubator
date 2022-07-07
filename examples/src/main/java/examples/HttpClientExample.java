package examples;

import io.vertx.await.Async;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;

import static io.vertx.await.Async.await;

public class HttpClientExample {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    HttpServer server = vertx.createHttpServer();
    server.requestHandler(request -> {
      request.response().end("Hello World");
    });
    Future<HttpServer> fut = server.listen(8080, "localhost");

    Async async = new Async(vertx);
    async.run(v -> {
      // Run on a Vert.x a virtual thread

      // Make sure server is started
      await(fut);

      // Make a simple HTTP request
      HttpClient client = vertx.createHttpClient();
      HttpClientRequest req = await(client.request(HttpMethod.GET, 8080, "localhost", "/"));
      HttpClientResponse resp = await(req.send());
      int status = resp.statusCode();
      Buffer body = await(resp.body());
      System.out.println("Got response '" + body + "'");
    });
  }
}
