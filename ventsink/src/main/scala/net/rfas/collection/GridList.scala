package net.rfas.collection

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import collection.mutable.ListBuffer
import java.util.UUID
import net.rfas.GridCoreOps

class GridList[+A](seqList: List[A]) {

  GridCoreOps.noop

  def map[B](f: (A) => B): List[B] = {
    remotelyApply(f).map(_._2)
  }

  def filter(p: (A) => Boolean): List[A] = {
    for (result <- remotelyApply(p) if result._2) yield seqList(result._1)
  }

  private def remotelyApply[T](f: (A) => T): List[(Int, T)] = {
    val signature = getSignature(f)
    val sUUID = UUID.randomUUID

    for (i <- 0 until seqList.size) {
      val baos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(baos)

      try {
        oos.writeObject(sUUID)
        oos.writeInt(i)
        oos.writeObject(seqList(i))
        oos.writeObject(signature)
        oos.writeObject(f)
      } catch {
          case e: Exception => e.printStackTrace //TODO handle this better
      } finally {
        oos.close
      }

      GridCoreOps.send(sUUID, baos.toByteArray)
    }

    val results = new ListBuffer[(Int, T)]
    while (results.size < seqList.size) {
      results += GridCoreOps.receive(sUUID).asInstanceOf[(Int, T)]
    }
    GridCoreOps.done(sUUID)

    results.result.sortBy(_._1)
  }

  private def getSignature[T](f: T)(implicit m: ClassManifest[T]) = m.toString
}