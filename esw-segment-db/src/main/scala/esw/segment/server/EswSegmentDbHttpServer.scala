package esw.segment.server

import buildinfo.BuildInfo

object EswSegmentDbHttpServer extends App {

  private case class Options(port: Int = 9192, testMode: Boolean = false)

  private val parser = new scopt.OptionParser[Options]("esw-segment-db") {
    head("esw-segment-db", BuildInfo.version)

    opt[Int]("port") valueName "<number>" action { (x, c) =>
      c.copy(port = x)
    } text s"The port number to use for the server (default: 9192)"

    opt[Unit]('t', "testMode") action { (_, c) =>
      c.copy(testMode = true)
    } text s"Use a test database instead of the normal one"
  }

  // Parse the command line options
  parser.parse(args, Options()) match {
    case Some(options) =>
      try {
        run(options)
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
          System.exit(1)
      }
    case None => System.exit(1)
  }

  // Run the application
  private def run(options: Options): Unit = {
    import esw.segment.db.DbWiring.*
    val dbName = if (options.testMode) testDbName else defaultDbName
    val wiring = new ServerWiring(options.port, dbName)
    wiring.server.start()
  }
}
