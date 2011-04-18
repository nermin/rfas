package net.rfas.collection

/**
   * Wrapper around sequential list
   */
class GridList[+A](seqList: List[A]) {
  def map[B](f: (A) => B): List[B] = {
    seqList.map(f)
  }
}