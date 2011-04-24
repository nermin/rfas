package net.rfas

import collection.GridList

object GridMethods {

  implicit def toGridMethod[A](l: List[A]) = new GridListMethod(l)

  class GridListMethod[A](l: List[A]) {
    def grid = new GridList(l)
  }

  //TODO Error Handling
  //TODO Handle node failures
  //TODO Properly comment out the code
  //TODO Try running workers on different machines
  //TODO Refactor the code to eliminate redundant lines
  //TODO Use Executor Service to multi-thread the worker
  //TODO Implement more methods beyond map
  //TODO Replace hashmap/queue/sender/receiver with actors
  //TODO Remote Class Loading
  //TODO Documentation:
  //          0. ZeroMQ must be installed on both sides (and LD_LIBRARY_PATH defined)
  //          1. The client app must have ventsink jar on the classpath
  //          2. Worker must have client jar on the classpath (due to lack of remote class-loading)
}