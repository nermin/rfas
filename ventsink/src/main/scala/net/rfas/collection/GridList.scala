package net.rfas.collection

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import collection.mutable.ListBuffer
import java.util.UUID
import net.rfas.GridCoreOps

class GridList[+A](seqList: List[A]) {
  def map[B](f: (A) => B): List[B] = {
    val signature = getSignature(f)
    // to uniquely identify this grid collection
    val sUUID = UUID.randomUUID

    GridCoreOps.noop
    seqList.foreach (
      elem => {
        val baos = new ByteArrayOutputStream
        val oos = new ObjectOutputStream(baos)

        try {
          oos.writeObject(sUUID)
          oos.writeObject(elem)
          oos.writeObject(signature)
          oos.writeObject(f)
        } catch {
          case e: Exception => e.printStackTrace //TODO handle this better
        } finally {
          oos.close
        }

        GridCoreOps.send(sUUID, baos.toByteArray)
      }
    )

    val results = new ListBuffer[B]
    while (results.size < seqList.size) {
      results += GridCoreOps.receive(sUUID).asInstanceOf[B]
    }
    GridCoreOps.done(sUUID)

    results.result
  }

  private def getSignature[T](f: T)(implicit m: ClassManifest[T]) = m.toString
}