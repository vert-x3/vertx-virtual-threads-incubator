package io.vertx.await.impl;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Schedule context tasks for execution.
 *
 * Context tasks are serialized in a queue.
 */
public interface Scheduler extends Executor {

  /**
   * Signals the scheduler the current thread does not own anymore the scheduler permit.
   *
   * @return an object to schedule a task for execution, the scheduled tasks preempts the scheduler task queue
   */
  Consumer<Runnable> unschedule();

}
