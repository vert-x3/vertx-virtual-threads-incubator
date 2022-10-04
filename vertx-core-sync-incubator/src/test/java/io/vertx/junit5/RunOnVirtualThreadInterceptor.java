package io.vertx.junit5;

import io.vertx.core.sync.Vertx;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RunOnVirtualThreadInterceptor implements InvocationInterceptor {

  @Override
  public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

    Optional<Object> target = invocationContext.getTarget();
    if (target.isPresent() && target.get() instanceof VirtualThreadTestBase) {
      Vertx vertx = ((VirtualThreadTestBase)target.get()).vertx;
      CompletableFuture<Void> result = new CompletableFuture<>();
      vertx.execute(v -> {
        try {
          InvocationInterceptor.super.interceptTestMethod(invocation, invocationContext, extensionContext);
          result.complete(null);
        } catch (Throwable e) {
          result.completeExceptionally(e);
        }
      });
      try {
        result.get(1, TimeUnit.MINUTES);
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    } else {
      InvocationInterceptor.super.interceptTestMethod(invocation, invocationContext, extensionContext);
    }
  }
}
