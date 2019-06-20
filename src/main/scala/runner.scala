import java.util.concurrent.LinkedBlockingQueue
import scala.sys.process.{Process, ProcessBuilder, ProcessLogger}


object StreamProcessLogger {
  private val nonzeroException = true

  def run(processBuilder: ProcessBuilder): (Process, Stream[String]) = {
    val logger = new StreamProcessLogger
    val process = processBuilder.run(logger)

    waitForExitInAnotherThread(process, logger)
    (process, logger.stream)
  }

  private def waitForExitInAnotherThread(process: Process, logger: StreamProcessLogger) = {
    val thread = new Thread() {
      override def run() = {
        logger.setExitCode(process.exitValue())
      }
    }
    thread.start()
  }
}

private class StreamProcessLogger extends ProcessLogger {
  val queue = new LinkedBlockingQueue[Either[Int, String]]

  override def buffer[T](f: => T): T = f

  override def out(s: => String): Unit = queue.put(Right(s))

  override def err(s: => String): Unit = queue.put(Right(s))

  def stream = next()

  def setExitCode(exitCode: Int) = queue.put(Left(exitCode))

  private def next(): Stream[String] = queue.take match {
    case Left(0) => Stream.empty
    case Left(code) => if (StreamProcessLogger.nonzeroException) scala.sys.error("Nonzero exit code: " + code) else Stream.empty
    case Right(s) => Stream.cons(s, next())
  }
}

object StreamProcessLoggerHelp {
  def executeCommand(command: String) {
    val (process, stream) = StreamProcessLogger.run(Process(command))
    stream.foreach(info => println("info==>" + info))
  }

  def main(args: Array[String]): Unit = {
    //StreamProcessLoggerHelp.executeCommand("""C:\Program Files\OpenSSH\bin\ssh -i C:\Users\ybwang1\.ssh\id_rsa_test au9uusr@cdts99hdbe13d.rxcorp.com 'bash -s' < /acceptance/au9/apps/gazelle/sparkTest.sh""")
    StreamProcessLoggerHelp.executeCommand("""C:\Program Files\OpenSSH\bin\ssh -i C:\Users\ybwang1\.ssh\id_rsa_test au9uusr@cdts99hdbe13d.rxcorp.com '/acceptance/au9/apps/gazelle/sparkTest.sh'""")
  }
}