package net.rfas

import collection.GridList

object GridMethods {

  implicit def toGridMethod[A](l: List[A]) = new GridListMethod(l)

  class GridListMethod[A](l: List[A]) {
    def grid = new GridList(l)
  }
}