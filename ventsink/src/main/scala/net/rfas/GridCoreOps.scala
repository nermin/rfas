package net.rfas

import java.util.UUID
import org.zeromq.ZMQ
import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util.concurrent._
;
object GridCoreOps {
  private val gridOps = new ConcurrentHashMap[UUID, BlockingQueue[(Int, AnyRef)]]
  private val executor = Executors.newSingleThreadExecutor
  private val context = ZMQ.context(3)
  private val sender = context.socket(ZMQ.PUSH)
  private val receiver = context.socket(ZMQ.PULL)
  private val timeout = System.getProperty("worker.timeout.millis").toLong
  @volatile private var keepListening = true

  // primary constructor begin
  sender.bind("tcp://*:" + System.getProperty("request.bind")) //TODO define constant
  receiver.bind("tcp://*:" + System.getProperty("response.bind"))
  listen
  Thread.sleep(100) // allow workers to connect to ventilator
  // primary constructor end

  def noop = {} // just so object gets constructed eagerly

  def send(uuid: UUID, payload: Array[Byte]) = {
    gridOps.putIfAbsent(uuid, new LinkedBlockingQueue[(Int, AnyRef)])

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
    gridOps.get(uuid).poll(timeout, TimeUnit.MILLISECONDS)
  }

  def done(uuid: UUID) = {
    gridOps.remove(uuid)
    keepListening = false
    executor.shutdown
  }

  private def listen = {

    val listener = new Runnable {
      def run = {
        while(keepListening)  {
          val ois = new ObjectInputStream(new ByteArrayInputStream(receiver.recv(0)))
          try {
            val uuid = ois.readObject.asInstanceOf[UUID]
            val queue = gridOps.get(uuid)
            /*
                      *  if queue is null, it has already been removed, which means this result has already been received
                      *  so just discard it
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
    executor.execute(listener)
  }
}