import java.io.File
import sbt._
class ZeroMQ(info: ProjectInfo) extends DefaultProject(info) {
  System.setProperty("push.bind", "5557")

  val zmqLib = new File("/usr/local/share/java/zmq.jar")
  val zmqLibPath = Path.fromFile(zmqLib)
  override def unmanagedClasspath = super.unmanagedClasspath +++ zmqLibPath
}