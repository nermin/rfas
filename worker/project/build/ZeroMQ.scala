import java.io.File
import sbt._

class ZeroMQ(info: ProjectInfo) extends DefaultProject(info) {
  System.setProperty("request.bind", "5557")
  System.setProperty("response.bind", "5558")
  // On Linux, this also needs to be specified in LD_LIBRARY_PATH:
  System.setProperty("java.library.path", "/usr/local/lib")
  System.setProperty("worker.threads", "2")


  val zmqLib = new File("/usr/local/share/java/zmq.jar")
  val zmqLibPath = Path.fromFile(zmqLib)
  override def unmanagedClasspath = super.unmanagedClasspath +++ zmqLibPath
  override def mainClass = Some("net.rfas.Worker")
}