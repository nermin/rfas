package net.rfas

import org.zeromq.ZMQ
import java.io.{ObjectOutputStream, ByteArrayOutputStream, ByteArrayInputStream, ObjectInputStream}

object Worker {
  def main(args: Array[String]): Unit = {
    val PATTERN = """\d+""".r

    val context = ZMQ.context(3)
    val receiver = context.socket(ZMQ.PULL)
    //TODO will binding to localhost work in a grid?
    receiver.connect("tcp://127.0.0.1:" + System.getProperty("request.bind"))

    val sender = context.socket(ZMQ.PUSH)
    sender.connect("tcp://127.0.0.1:" + System.getProperty("response.bind"))

    while (true) {
      val ois = new ObjectInputStream(new ByteArrayInputStream(receiver.recv(0)))
      val baos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(baos)

      try {
        val elem = ois.readObject
        val signature = ois.readObject.asInstanceOf[String]
        val fTypeIndex = PATTERN.findFirstIn(signature).get.toInt
        val receivedF = ois.readObject
        val result: Option[AnyRef] = fTypeIndex match {
          // for now only handling application of functions with 1 parameter
          case 1 => Some(receivedF.asInstanceOf[{def apply[T,R](v1: T):R}].apply(elem))
          case _ => None
        }
        println("worker calculated: " + result.get)

        oos.writeObject(result.get)

      } catch {
        case e: Exception => e.printStackTrace //TODO handle this better
      } finally {
        ois.close
        oos.close
      }

      sender.send(baos.toByteArray, 0)
    }
  }
}