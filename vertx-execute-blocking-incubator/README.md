## Vert.x execute blocking task on virtual threads

Run blocking tasks on a virtual thread instead of a platform thread.

### Examples

```java
// Assuming you are on a vertx thread
Future<String> fut = ExecuteBlocking.executeBlocking(() -> {
  Thread.sleep(1000);
  return "Hello";
});
fut.onComplete(onSuccess(s -> {
  System.out.println(s);
}));
```

You can view all [examples](vertx-async-await-incubator/src/main/java/examples/AsyncExamples.java).
