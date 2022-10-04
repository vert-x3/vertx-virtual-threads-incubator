package io.vertx.junit5;

import io.vertx.core.sync.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RunOnVirtualThreadInterceptor.class)
public abstract class VirtualThreadTestBase {

  protected Vertx vertx;

  @BeforeEach
  public void before() {
    vertx = new Vertx();
  }

  @AfterEach
  public void after() {
    try {
      vertx.close();
    } finally {
      vertx = null;
    }
  }
}
