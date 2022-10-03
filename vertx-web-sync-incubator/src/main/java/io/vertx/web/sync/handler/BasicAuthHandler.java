package io.vertx.web.sync.handler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.web.sync.RoutingContext;
import io.vertx.web.sync.WebHandler;

import java.nio.charset.StandardCharsets;

import static io.vertx.ext.auth.impl.Codec.base64Decode;

public class BasicAuthHandler implements WebHandler {

  private final io.vertx.auth.sync.authentication.AuthenticationProvider provider;

  public BasicAuthHandler(AuthenticationProvider provider) {
    this.provider = new io.vertx.auth.sync.authentication.AuthenticationProvider(provider);
  }

  @Override
  public HandlerReturn handle(RoutingContext ctx) {
    String authorization = parseAuthorization(ctx);

    final String suser;
    final String spass;

    // decode the payload
    String decoded = new String(base64Decode(authorization), StandardCharsets.UTF_8);

    int colonIdx = decoded.indexOf(":");
    if (colonIdx != -1) {
      suser = decoded.substring(0, colonIdx);
      spass = decoded.substring(colonIdx + 1);
    } else {
      suser = decoded;
      spass = null;
    }

    User user = provider.authenticate(new UsernamePasswordCredentials(suser, spass));

    System.out.println(user.principal());
    // TODO: update context
    return HandlerReturn.NEXT;
  }

  protected final String parseAuthorization(RoutingContext ctx) {

    final String authorization = ctx.getHeader(HttpHeaders.AUTHORIZATION);

    int idx = authorization.indexOf(' ');

    if (idx <= 0) {
      throw new RuntimeException("BAD_REQUEST");
    }

    return authorization.substring(idx + 1);
  }
}
