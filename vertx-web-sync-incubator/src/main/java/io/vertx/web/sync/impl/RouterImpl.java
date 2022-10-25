package io.vertx.web.sync.impl;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.sync.http.HttpServerRequest;
import io.vertx.web.sync.Router;
import io.vertx.web.sync.RouterOptions;
import io.vertx.web.sync.WebHandler;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class RouterImpl implements Router {

  private final RouterOptions opts;
  private final Map<HttpMethod, CTrie<WebHandler>> trees;

  public RouterImpl(RouterOptions opts) {
    if (opts.getPrefix() != null && opts.getPrefix().charAt(0) != '/') {
      throw new IllegalArgumentException("prefix must begin with '/' in path");
    }

    this.opts = opts;
    this.trees = new IdentityHashMap<>();
  }

  @Override
  public Router on(HttpMethod method, String path, WebHandler... handlers) {
    if (path.charAt(0) != '/') {
      throw new IllegalArgumentException("path must begin with '/' in path");
    }

    if (!trees.containsKey(method)) {
      trees.put(method, new CTrie<>());
    }

    if (opts.getPrefix() != null) {
      path = opts.getPrefix() + path;
    }

    final CTrie<WebHandler> values = trees.get(method);
    for (WebHandler h : handlers) {
      values.add(path, h);
    }

    return this;
  }

  private LList<WebHandler> find(RoutingContextInternal ctx) {
    final HttpMethod verb = ctx.method();
    final CTrie<WebHandler> tree = trees.get(verb);
    if (tree != null) {
      return tree.search(ctx);
    }

    return null;
  }

  @Override
  public void handle(HttpServerRequest req) {
    final RoutingContextImpl ctx = new RoutingContextImpl(req);

    final LList<WebHandler> needle = find(ctx);

    if (needle == null) {
//      final Handler<Context> handle405 = opts.getMethodNotAllowedHandler();
//
//      if (handle405 != null) {
//        for (HttpMethod key : trees.keySet()) {
//          if (key == ctx.getVerb()) {
//            continue;
//          }
//
//          final Node tree = trees.get(key);
//          // in this case we lookup as we don't want
//          // to reparse the params
//          if (tree.lookup(ctx) != null) {
//            ctx.getResponse().setStatusCode(405);
//            handle405.handle(ctx);
//            return;
//          }
//        }
//      }
    } else {
      needle.forEach(handler -> {
        try {
          switch (handler.handle(ctx)) {
            case NEXT:
              return true;
            case END:
              return false;
            default:
              throw new IllegalStateException("Not Implemented");
          }
        } catch (RuntimeException e) {
          e.printStackTrace();
          return false;
        }
      });
    }
  }
}
