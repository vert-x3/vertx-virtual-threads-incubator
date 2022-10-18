package io.vertx.await.impl;

import io.vertx.core.http.HttpServer;

public class Main {

  public static void main(String[] args) {

    HttpServer server = null;
    server.requestHandler(req -> {

      // virtual thread
      req.response().end(); // message netty
    });

  }
}
