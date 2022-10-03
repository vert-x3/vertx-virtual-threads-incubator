package io.vertx.auth.sync.authentication;

import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;

import static io.vertx.await.Async.await;

public class AuthenticationProvider {

  private final io.vertx.ext.auth.authentication.AuthenticationProvider delegate;


  public AuthenticationProvider(io.vertx.ext.auth.authentication.AuthenticationProvider delegate) {
    this.delegate = delegate;
  }

  /**
   * Authenticate a user.
   * <p>
   * The first argument is a Credentials object containing information for authenticating the user.
   * What this actually contains depends on the specific implementation.
   *
   * @see io.vertx.ext.auth.authentication.AuthenticationProvider#authenticate(Credentials, Handler)
   * @param credentials  The credentials
   * @return The result future
   */
  public User authenticate(Credentials credentials) {
    return await(delegate.authenticate(credentials));
  }
}
