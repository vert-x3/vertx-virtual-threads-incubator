## Vert.x synchronous API

A synchronous Vert.x API

### Examples

```java
vertx.execute(v -> {
  // Run on a Vert.x a virtual thread
  HttpClientRequest request = client.request(8080, "localhost", "GET", "/");
  request.end();
  HttpClientResponse response = request.response();
  assertEquals(200, response.statusCode());
  assertEquals("Hello World", response.body().toString());
});
```
