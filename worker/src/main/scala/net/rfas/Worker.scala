package net.rfas

import org.zeromq.ZMQ
import java.io.{ObjectOutputStream, ByteArrayOutputStream, ByteArrayInputStream, ObjectInputStream}
import actors.scheduler.DaemonScheduler
import scala.actors._

object Worker {
  private val context = ZMQ.context(3)
  private val receiver = context.socket(ZMQ.PULL)
  private val executor = new BoundedExecutor(System.getProperty("worker.threads").toInt)
  private case class Send(payload: Array[Byte])

  receiver.connect("tcp://" + System.getProperty("bind.address") +":" + System.getProperty("request.bind"))

  /*
      ZeroMQ sockets are not thread-safe. Although this will make things slower - sequential,,
      sending a result of a function over wire should take much less time then applying it.
      (This assumes function application is computationally/IO intensive)
    */
  private object Sender extends Actor {
    private val senderSocket = context.socket(ZMQ.PUSH)
    override def scheduler = DaemonScheduler

    def act = {
      senderSocket.connect("tcp://" + System.getProperty("bind.address") + ":" + System.getProperty("response.bind"))
      while (true) {
        receive {
          case Send(payload) => senderSocket.send(payload, 0)
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val PATTERN = """\d+""".r
    Sender.start
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

  def send(payload: Array[Byte]) = Sender ! Send(payload)
}