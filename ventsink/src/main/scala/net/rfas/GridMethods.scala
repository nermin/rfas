package net.rfas

import collection.GridList
import org.zeromq.ZMQ

object GridMethods {

  // primary constructor begin
  private val context = ZMQ.context(3)
  private val sender = context.socket(ZMQ.PUSH)
  sender.bind("tcp://*:" + System.getProperty("request.bind")) //TODO define constant

  private val receiver = context.socket(ZMQ.PULL)
  receiver.bind("tcp://*:" + System.getProperty("response.bind"))

  Thread.sleep(100) // allow workers to connect to ventilator
  // primary constructor end

  implicit def toGridMethod[A](l: List[A]) = new GridListMethod(l)

  class GridListMethod[A](l: List[A]) {
    def grid = new GridList(l, sender, receiver)
  }

  //TODO Error Handling
  //TODO Handle node failures
  //TODO Try multiple workers
  //TODO Properly comment out the code
  //TODO Try running workers on different machines
  //TODO Refactor the code to eliminate redundant lines
  //TODO Use Executor Service to multi-thread the worker
  //TODO Documentation:
  //          0. ZeroMQ must be installed on both sides (and LD_LIBRARY_PATH defined)
  //          1. The client app must have ventsink jar on the classpath
  //          2. Worker must have client jar on the classpath (due to lack of remote class-loading)
}