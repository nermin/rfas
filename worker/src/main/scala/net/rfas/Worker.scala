package net.rfas

import org.zeromq.ZMQ
import java.io.{ByteArrayInputStream, ObjectInputStream}
;
object Worker {
  def main(args: Array[String]): Unit = {
    val PATTERN = """\d+""".r

    val context = ZMQ.context(3)
    val receiver = context.socket(ZMQ.PULL)
    //TODO will binding to localhost work in a grid?
    receiver.connect("tcp://localhost:" + System.getProperty("request.bind"))

    val sender = context.socket(ZMQ.PUSH)
    sender.connect("tcp://localhost:" + System.getProperty("response.bind"))

    while (true) {
      val ois = new ObjectInputStream(new ByteArrayInputStream(receiver.recv(0)))

      try {
        val elem = ois.readObject
        val signature = ois.readObject.asInstanceOf[String]
        val fTypeIndex = PATTERN.findFirstIn(signature).get.toInt
        val receivedF = ois.readObject
        val result: Option[AnyRef] = fTypeIndex match {
          case 1 => Some(receivedF.asInstanceOf[{def apply[T,R](v1: T):R}].apply(elem))
          case _ => None
        }
        println("RESULT: " + result.get)
        println("CLASS: " + result.get.getClass)
      } catch {
        case e: Exception => e.printStackTrace //TODO handle this better
      } finally {
        ois.close
      }
    }
  }
}