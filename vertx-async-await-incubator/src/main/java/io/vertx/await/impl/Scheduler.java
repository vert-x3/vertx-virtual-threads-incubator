package io.vertx.await.impl;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface Scheduler extends Executor {

  boolean inThread();

  Consumer<Runnable> detach();

}
