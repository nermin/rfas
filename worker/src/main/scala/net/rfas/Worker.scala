package net.rfas

import org.zeromq.ZMQ
import java.io.{ObjectOutputStream, ByteArrayOutputStream, ByteArrayInputStream, ObjectInputStream}
import java.util.concurrent.Executors

object Worker {
  val context = ZMQ.context(3)
  val receiver = context.socket(ZMQ.PULL)
  val sender = context.socket(ZMQ.PUSH)
  val executor = new BoundedExecutor(System.getProperty("worker.threads").toInt)

  // primary constructor begin
  //TODO will binding to localhost work in a grid?
  receiver.connect("tcp://127.0.0.1:" + System.getProperty("request.bind"))
  sender.connect("tcp://127.0.0.1:" + System.getProperty("response.bind"))
  // primary constructor end

  def main(args: Array[String]): Unit = {
    val PATTERN = """\d+""".r

    println("waiting for something to work on")

    while (true) {
      val payload = receiver.recv(0)
      println("  worker received message")
      val functionApplication = new Runnable {
        def run = {
          val ois = new ObjectInputStream(new ByteArrayInputStream(payload))
          val baos = new ByteArrayOutputStream
          val oos = new ObjectOutputStream(baos)

          try {
            val uuid = ois.readObject
            val index = ois.readInt
            val elem = ois.readObject
            val signature = ois.readObject.asInstanceOf[String]
            val fTypeIndex = PATTERN.findFirstIn(signature).get.toInt
            val receivedF = ois.readObject
            println("    worker starting to apply to: " + elem)
            val result: Option[AnyRef] = fTypeIndex match {
              // for now only handling application of functions with 1 parameter
              case 1 => Some(receivedF.asInstanceOf[{def apply[T,R](v1: T):R}].apply(elem))
              case _ => None
            }
            println("result of worker application: " + result.get)

            oos.writeObject(uuid)
            oos.writeInt(index)
            oos.writeObject(result.get)

          } catch {
            case e: Exception => e.printStackTrace //TODO handle this better
          } finally {
            ois.close
            oos.close
          }

          send(baos.toByteArray)
        }
      }
      executor.submitTask(functionApplication)
    }
  }

  def send(payload: Array[Byte]) = {
    synchronized {
      /*
            ZeroMQ sockets are not thread-safe. Although this will make things slower,
            sending a function over wire should take much less time then applying it.
            (This assumes function application is computationally/IO intensive)
          */
      sender.send(payload, 0)
    }
  }
}