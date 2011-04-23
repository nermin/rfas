package net.rfas.collection

import org.zeromq.ZMQ
import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}
import collection.mutable.ListBuffer
import java.util.concurrent.{Executors, Callable, FutureTask}

class GridList[+A](seqList: List[A], sender: ZMQ.Socket, receiver: ZMQ.Socket) {


  def map[B](f: (A) => B): List[B] = {
    val signature = getSignature(f)
    val executor = Executors.newFixedThreadPool(1)

    val result = new FutureTask[List[B]](
      new Callable[List[B]]() {
        def call: List[B] = {
          val resultsCollected = new ListBuffer[B]
          while (resultsCollected.size < seqList.size) {
            val ois = new ObjectInputStream(new ByteArrayInputStream(receiver.recv(0)))
            try {
              resultsCollected += ois.readObject.asInstanceOf[B]
            } catch {
              case e: Exception => e.printStackTrace //TODO handle better
            } finally {
              ois.close
            }
          }
          resultsCollected.result
        }
      }
    )

    executor.execute(result)

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

    result.get
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