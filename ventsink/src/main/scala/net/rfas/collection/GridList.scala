package net.rfas.collection

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import collection.mutable.ListBuffer
import java.util.UUID
import net.rfas.GridCoreOps

class GridList[+A](seqList: List[A]) {

  GridCoreOps.init

  def map[B](f: (A) => B): List[B] = {
    remotelyApply(f).map(_._2)
  }

  def filter(p: (A) => Boolean): List[A] = {
    remotelyApply(p).filter(_._2).map(e => seqList(e._1))
  }

//  def flatMap[B](f: (A) => Traversable[B]): List[B] = {
//
//  }

  private def remotelyApply[T](f: (A) => T): List[(Int, T)] = {
    val signature = getSignature(f)
    val sUUID = UUID.randomUUID

    GridCoreOps.ready(sUUID)
    for (i <- 0 until seqList.size) {
      sendElem(sUUID, i, signature, f)
    }

    val results = new ListBuffer[(Int, T)]
    while (results.size < seqList.size) {
      val result = GridCoreOps.receive(sUUID)
      if (result == null) {
        // timeout occurred, resend
        resendMissing(sUUID, signature, f, results.result.map(_._1))
      } else {
        val received = result.asInstanceOf[(Int, T)]
        // received result can be a duplicate, which means there was a resend
        if (!results.contains(received)) {
          results += received
        }
      }
    }
    GridCoreOps.done(sUUID)

    results.result.sortBy(_._1)
  }

  private def getSignature[T](f: T)(implicit m: ClassManifest[T]) = m.toString

  private def sendElem[T](uuid: UUID, index: Int, signature: String, f: (A) => T) = {
    val baos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(baos)

    try {
      oos.writeObject(uuid)
      oos.writeInt(index)
      oos.writeObject(seqList(index))
      oos.writeObject(signature)
      oos.writeObject(f)
    } catch {
      case e: Exception => e.printStackTrace //TODO handle this better
    } finally {
      oos.close
    }

      GridCoreOps.send(baos.toByteArray)
  }

  private def resendMissing[T](uuid: UUID, signature: String, f: (A) => T, processed: List[Int]) = {
    val missing = for (i <- 0 until seqList.size if !processed.contains(i)) yield i
    println("Resending: " + missing)
    for (missed <- missing) {
      sendElem(uuid, missed, signature, f)
    }
  }
}