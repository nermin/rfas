package net.rfas

import java.util.UUID
import org.zeromq.ZMQ
import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util.concurrent._
import scala.actors._
import scheduler.DaemonScheduler
import java.lang.Thread

object GridCoreOps {
  private val gridOps = new ConcurrentHashMap[UUID, BlockingQueue[(Int, AnyRef)]]
  private val context = ZMQ.context(3)
  private val timeout = System.getProperty("worker.timeout.millis").toLong

  private case class Send(payload: Array[Byte])

  /*
      ZeroMQ sockets are not thread-safe. Although this will make things slower,
      sending a function over wire should take much less time then applying it.
      (This assumes function application is computationally/IO intensive)
    */
  private object Sender extends Actor {
    private val senderSocket = context.socket(ZMQ.PUSH)
    override def scheduler = DaemonScheduler

    def act = {
      senderSocket.bind("tcp://*:" + System.getProperty("request.bind")) //TODO define constant
      while (true) {
        receive {
          case Send(payload) => senderSocket.send(payload, 0)
        }
      }
    }
  }

  private object Receiver extends Thread {
    private val receiverSocket = context.socket(ZMQ.PULL)

    override def run = {
      receiverSocket.bind("tcp://*:" + System.getProperty("response.bind"))
      while (true) {
        val ois = new ObjectInputStream(new ByteArrayInputStream(receiverSocket.recv(0)))
        try {
          val uuid = ois.readObject.asInstanceOf[UUID]
          val queue = gridOps.get(uuid)
          /*
                      if queue is null, it has already been removed, which means this result has already been received
                      so just discard it
                    */
          if (queue != null) {
            queue.put((ois.readInt, ois.readObject))
          }
        } catch {
          case e: Exception => e.printStackTrace //TODO handle better
        } finally {
          ois.close
        }
      }
    }
  }

  def init = {
    Sender.start
    Receiver.setDaemon(true)
    Receiver.start
    Thread.sleep(250) // allow workers to connect to ventilator
  }

  def send(payload: Array[Byte]) = Sender ! Send(payload)

  def receive(uuid: UUID) = gridOps.get(uuid).poll(timeout, TimeUnit.MILLISECONDS)

  def ready(uuid: UUID) = gridOps.put(uuid, new LinkedBlockingQueue[(Int, AnyRef)])

  def done(uuid: UUID) = gridOps.remove(uuid)

}