package net.rfas

import collection.GridList

object GridMethods {

  implicit def toGridMethod[A](l: List[A]) = new GridListMethod(l)

  class GridListMethod[A](l: List[A]) {
    def grid = new GridList(l)
  }

  //TODO Send the result back to ventsink
  //TODO Try multiple workers
  //TODO Properly comment out the code
  //TODO Try running workers on different machines
  //TODO Refactor the code to eliminate redundant lines
}