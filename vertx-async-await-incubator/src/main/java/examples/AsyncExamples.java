package examples;

import io.vertx.await.Async;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;

import static io.vertx.await.Async.await;

public class AsyncExamples {

  public void example(Vertx vertx) {
    Async async = new Async(vertx);
    async.run(v -> {
      // Run on a Vert.x a virtual thread
      HttpServer server = vertx.createHttpServer();
      server.requestHandler(request -> {
        request.response().end("Hello World");
      });
      await(server.listen(8080, "localhost"));
      HttpClient client = vertx.createHttpClient();
      HttpClientRequest req = await(client.request(HttpMethod.GET, 8080, "localhost", "/"));
      HttpClientResponse resp = await(req.send());
      int status = resp.statusCode();
      Buffer body = await(resp.body());
    });
  }
}
