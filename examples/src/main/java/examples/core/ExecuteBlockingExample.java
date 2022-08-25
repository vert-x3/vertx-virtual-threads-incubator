package examples.core;

import io.vertx.await.Async;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.executeblocking.ExecuteBlocking;

import static io.vertx.await.Async.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class ExecuteBlockingExample {

  private static final Vertx vertx = Vertx.vertx();

  public static void main(String[] args) throws Exception {

    Async async = new Async(vertx);

    int num = 100_000;

    async.run(v -> {
      CyclicBarrier barrier = new CyclicBarrier(num);
      List<Future> futures = new ArrayList<>(num);
      for (int i = 0;i < num;i++) {
        Future<String> fut = ExecuteBlocking.executeBlocking(() -> {
          barrier.await();
          return "Hello";
        });
        futures.add(fut);
      }
      CompositeFuture res = await(CompositeFuture.join(futures));
      System.out.println("Got result");
    });

    // Need that since no vertx thread is started at all.
    System.in.read();
  }
}
