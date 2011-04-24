package net.rfas.collection

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import collection.mutable.ListBuffer
import java.util.UUID
import net.rfas.GridCoreOps

class GridList[+A](seqList: List[A]) {

  GridCoreOps.noop

  def map[B](f: (A) => B): List[B] = {
    val signature = getSignature(f)
    // to uniquely identify this grid collection
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

    val results = new ListBuffer[(Int, B)]
    while (results.size < seqList.size) {
      results += GridCoreOps.receive(sUUID).asInstanceOf[(Int, B)]
    }
    GridCoreOps.done(sUUID)

    results.result.sortBy(_._1).map(_._2)
  }

  def filter(p: (A) => Boolean): List[A] = {
    val signature = getSignature(p)
    val sUUID = UUID.randomUUID

    for (i <- 0 until seqList.size) {
      val baos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(baos)

      try {
        oos.writeObject(sUUID)
        oos.writeInt(i)
        oos.writeObject(seqList(i))
        oos.writeObject(signature)
        oos.writeObject(p)
      } catch {
          case e: Exception => e.printStackTrace //TODO handle this better
      } finally {
        oos.close
      }

      GridCoreOps.send(sUUID, baos.toByteArray)
    }

    val results = new ListBuffer[(Int, Boolean)]
    while (results.size < seqList.size) {
      results += GridCoreOps.receive(sUUID).asInstanceOf[(Int, Boolean)]
    }
    GridCoreOps.done(sUUID)

    for (result <- results.result.sortBy(_._1) if result._2) yield seqList(result._1)
  }

  private def getSignature[T](f: T)(implicit m: ClassManifest[T]) = m.toString
}