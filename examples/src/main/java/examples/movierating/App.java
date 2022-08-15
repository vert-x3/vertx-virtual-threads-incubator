package examples.movierating;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.List;

import static io.vertx.await.Async.await;

public class App {

  private static JDBCPool client;
  private static Vertx vertx;
  private static Async async;

  public static void main(String[] args) throws Exception {
    vertx = Vertx.vertx();
    async = new Async(vertx);
    client = JDBCPool.pool(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size-loop", 30)
    );
    async.run(v -> start());
    System.in.read();
  }

  public static void start() {
    List<String> statements = List.of(      "CREATE TABLE MOVIE (ID VARCHAR(16) PRIMARY KEY, TITLE VARCHAR(256) NOT NULL)",
      "CREATE TABLE RATING (ID INTEGER IDENTITY PRIMARY KEY, value INTEGER, MOVIE_ID VARCHAR(16))",
      "INSERT INTO MOVIE (ID, TITLE) VALUES 'starwars', 'Star Wars'",
      "INSERT INTO MOVIE (ID, TITLE) VALUES 'indianajones', 'Indiana Jones'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 1, 'starwars'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 5, 'starwars'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 9, 'starwars'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 10, 'starwars'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 4, 'indianajones'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 7, 'indianajones'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 3, 'indianajones'",
      "INSERT INTO RATING (VALUE, MOVIE_ID) VALUES 9, 'indianajones'"
    );
    statements.forEach(st -> {
      await(client.query(st).execute());
    });

    var router = Router.router(vertx);
    router.get("/movie/:id").handler(ctx -> getMovie(ctx));
    router.post("/rateMovie/:id").handler(ctx -> rateMovie(ctx));
    router.get("/getRating/:id").handler(ctx -> getRating(ctx));

    // Start the server
    await(vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080));

    System.out.println("server started");
  }

  private static void getMovie(RoutingContext ctx) {
    var id = ctx.pathParam("id");
    var rows = await(client.preparedQuery("SELECT TITLE FROM MOVIE WHERE ID=?").execute(Tuple.of(id)));
    if (rows.size() == 1) {
      ctx.response().end(new JsonObject().put("id", id).put("title", rows.iterator().next().getString("TITLE")).encode());
    } else {
      ctx.response().setStatusCode(404).end();
    }
  }

  private static void rateMovie(RoutingContext ctx) {
    var movie = ctx.pathParam("id");
    var rating = Integer.parseInt(ctx.queryParam("rating").get(0));
    var rows = await(client.preparedQuery("SELECT TITLE FROM MOVIE WHERE ID=?").execute(Tuple.of(movie)));
    if (rows.size() == 1) {
      await(client.preparedQuery("INSERT INTO RATING (VALUE, MOVIE_ID) VALUES ?, ?").execute(Tuple.of(rating, movie)));
      ctx.response().setStatusCode(200).end();
    } else {
      ctx.response().setStatusCode(404).end();
    }
  }

  private static void getRating(RoutingContext ctx) {
    var id = ctx.pathParam("id");
    var rows = await(client.preparedQuery("SELECT AVG(VALUE) AS VALUE FROM RATING WHERE MOVIE_ID=?").execute(Tuple.of(id)));
    ctx.response().end(new JsonObject().put("id", id).put("rating", rows.iterator().next().getDouble("VALUE")).encode());
  }
}
