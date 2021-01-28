package esw.segment.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(port: Int, routes: Route)(implicit typedSystem: ActorSystem[SpawnProtocol.Command], ec: ExecutionContextExecutor) {
  def start(): Future[Http.ServerBinding] = {
    val f = Http().newServerAt("0.0.0.0", port).bind(routes)
    f.onComplete {
      case Success(b) =>
        println(s"Server online at http://localhost:${b.localAddress.getPort}")
        println(s"For HTTP API docs see: http://localhost:${b.localAddress.getPort}/docs")
      case Failure(ex) =>
        ex.printStackTrace()
    }
    f
  }
}
