package net.rfas

import java.util.concurrent.{RejectedExecutionException, Executors, Semaphore}

/*
   Based on Listing 8.4 from Brian Goetz's Java concurrency in practice book
   ZeroMQ buffers all inbound messages anyways, so no need to queue all of them up in the executor.
   Instead, allow only as many functions to be concurrently applied as the number of allocated threads.
 */
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