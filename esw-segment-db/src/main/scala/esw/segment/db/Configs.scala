package esw.segment.db

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.Config

class Configs(_port: Option[Int])(implicit typedSystem: ActorSystem[SpawnProtocol.Command]) {
  private lazy val config: Config = typedSystem.settings.config

  lazy val port: Int = _port.getOrElse(config.getConfig("server").getInt("port"))
}
