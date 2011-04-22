package net.rfas.collection

import org.zeromq.ZMQ
import java.io.{ByteArrayOutputStream, ObjectOutputStream}

class GridList[+A](seqList: List[A]) {
  def map[B](f: (A) => B): List[B] = {
    val signature = getSignature(f)
    val context = ZMQ.context(3)
    val sender = context.socket(ZMQ.PUSH)
    sender.bind("tcp://*:" + System.getProperty("request.bind")) //TODO define constant


    seqList.foreach (
      elem => {
        val baos = new ByteArrayOutputStream
        val oos = new ObjectOutputStream(baos)

        try {
          oos.writeObject(elem)
          oos.writeObject(signature)
          oos.writeObject(f)
        } catch {
          case e: Exception => e.printStackTrace //TODO handle this better
        } finally {
          oos.close
        }

        sender.send(baos.toByteArray, 0)
      }



    )


    seqList.map(f)
  }

  private def getSignature[T](f: T)(implicit m: ClassManifest[T]) = m.toString

/*
  private def withObjectOutputStream(oos: ObjectOutputStream)(op: ObjectOutputStream => Unit) {
    try {
      op(oos)
    } finally {
      oos.close
    }
  }
  */
}