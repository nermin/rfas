package net.rfas

import java.util.UUID
import org.zeromq.ZMQ
import java.util.concurrent.{Executors, LinkedBlockingQueue, ConcurrentHashMap, BlockingQueue}
import java.io.{ByteArrayInputStream, ObjectInputStream}
;
object GridCoreOps {
  private val gridOps = new ConcurrentHashMap[UUID, BlockingQueue[AnyRef]]
  private val context = ZMQ.context(3)
  private val sender = context.socket(ZMQ.PUSH)
  private val receiver = context.socket(ZMQ.PULL)

  // primary constructor begin
  sender.bind("tcp://*:" + System.getProperty("request.bind")) //TODO define constant
  receiver.bind("tcp://*:" + System.getProperty("response.bind"))
  listen
  println("12323432453656")
  Thread.sleep(100) // allow workers to connect to ventilator
  // primary constructor end

  def noop = {} // just so object gets constructed eagerly

  def send(uuid: UUID, payload: Array[Byte]) = {
    gridOps.putIfAbsent(uuid, new LinkedBlockingQueue[AnyRef])

    synchronized {
      /*
            ZeroMQ sockets are not thread-safe. Although this will make things slower,
            sending a function over wire should take much less time then applying it.
            (This assumes function application is computationally/IO intensive)
          */
      sender.send(payload, 0)
    }
  }

  def receive(uuid: UUID) = {
    gridOps.get(uuid).take
  }

  def done(uuid: UUID) = {
    gridOps.remove(uuid)
  }

  private def listen = {
    val executor = Executors.newSingleThreadExecutor
    val listener = new Runnable {
      def run = {
        while(true)  {
          val ois = new ObjectInputStream(new ByteArrayInputStream(receiver.recv(0)))
          try {
            val uuid = ois.readObject.asInstanceOf[UUID]
            gridOps.get(uuid).put(ois.readObject)
          } catch {
            case e: Exception => e.printStackTrace //TODO handle better
          } finally {
            ois.close
          }
        }
      }
    }
    executor.execute(listener)
  }
}