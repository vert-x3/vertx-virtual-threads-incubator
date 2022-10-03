package io.vertx.web.sync;

@FunctionalInterface
public interface WebHandler {

  public enum HandlerReturn {
    REROUTE,
    END,
    NEXT
  }
  HandlerReturn handle(RoutingContext ctx);
}
