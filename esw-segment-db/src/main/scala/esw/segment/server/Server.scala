package esw.segment.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Server(port: Int, routes: Routes)(implicit typedSystem: ActorSystem[SpawnProtocol.Command]) {
  import typedSystem._

  def start(): Future[Http.ServerBinding] = {
    val f = Http().newServerAt("0.0.0.0", port).bind(routes.route)
    f.onComplete {
      case Success(b) =>
        println(s"Server online at http://${b.localAddress.getHostName}:${b.localAddress.getPort}")
      case Failure(ex) =>
        ex.printStackTrace()
    }
    f
  }
}
