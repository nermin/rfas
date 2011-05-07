/**
 * Based on Listing 8.4 from Brian Goetz's
 * Java concurrency in practice book
 */
package net.rfas

import java.util.concurrent.{RejectedExecutionException, Executors, Semaphore}

class BoundedExecutor(bound: Int) {
  val semaphore = new Semaphore(bound)
  val executor = Executors.newFixedThreadPool(bound)

  def submitTask(command: Runnable) = {
    semaphore.acquire
    try {
      executor.execute(new Runnable {
        def run = {
          try {
            command.run
          } finally {
            semaphore.release
          }
        }
      })
    } catch {
      case ex: RejectedExecutionException => semaphore.release
    }
  }
}