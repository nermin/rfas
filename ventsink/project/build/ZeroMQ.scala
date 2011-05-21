import java.io.File
import sbt._
class ZeroMQ(info: ProjectInfo) extends DefaultProject(info) {
  // outbound port:
  System.setProperty("request.bind", "5557")
  // inbound port:
  System.setProperty("response.bind", "5558")
  // Timeout value after which it is assumed a worker died and the same function application is resent:
  System.setProperty("worker.timeout.millis", "1000")
  // On Linux, this also needs to be specified in LD_LIBRARY_PATH:
  System.setProperty("java.library.path", "/usr/local/lib")


  val zmqLib = new File("/usr/local/share/java/zmq.jar")
  val zmqLibPath = Path.fromFile(zmqLib)
  override def unmanagedClasspath = super.unmanagedClasspath +++ zmqLibPath
}