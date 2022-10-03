## Vert.x Web synchronous API

A synchronous Vert.x Web API

### Examples

This new experiment is using a CTrie as a router, which improves overall performance
And shows how handlers can still be used.

This is still a early WiP but to showcase what we can do:


```java
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
```
