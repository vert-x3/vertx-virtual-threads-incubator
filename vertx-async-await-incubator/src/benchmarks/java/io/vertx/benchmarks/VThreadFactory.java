package io.vertx.benchmarks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class VThreadFactory {
  private static final MethodHandle virtualThreadFactory;

  static {
    MethodHandle vtf = null;
    try {
      MethodHandles.Lookup thr = MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());
      Class<?> vtbClass = Class.forName("java.lang.ThreadBuilders$VirtualThreadBuilder", false, null);
      vtf = thr.findConstructor(vtbClass, MethodType.methodType(void.class, Executor.class));
      // create efficient transformer
      vtf = vtf.asType(MethodType.methodType(Thread.Builder.OfVirtual.class, Executor.class));
    } catch (Exception | Error e) {
      // no good
      System.out.println("************************** Failed to initialize: " + e);
    }
    virtualThreadFactory = vtf;
  }

  public static ThreadFactory createThreadFactory(Executor carrier) {
    try {
      Thread.Builder.OfVirtual ov = (Thread.Builder.OfVirtual) virtualThreadFactory.invokeExact(carrier);
      return ov.factory();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }
}
