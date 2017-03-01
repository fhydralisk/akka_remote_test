package cn.edu.tsinghua.ee.fi.akka.remote_test

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.util.Try
import scala.concurrent.duration._

/**
  * Created by hydra on 2017/3/1.
  */
object App {
  def main(args: Array[String]): Unit = {
    Try((args(0), args(1))) map { case (host, role) =>
      val sendAfter = Try(args(2).toInt second) getOrElse ( 30 seconds )

      val config = ConfigFactory.load().withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname = '$host'"))
      val system = ActorSystem.create("RemoteTestSystem", config)
      role match {
        case "Sender" =>
          system.scheduler.scheduleOnce(sendAfter) {
            system.actorSelection(receiverPath) ! "Test message"
          }
        case "Receiver" =>
          system.actorOf(Receiver.props, name = "Receiver")
      }

    } getOrElse {
      printUsage
    }

  }

  def printUsage: Unit = {
    println("Usage: \nParameter: <host> <role> [send after (s)]")
  }

  def receiverPath: String = {
    "akka.tcp://RemoteTestSystem@odl2.nopqzip.com:2551/user/Receiver"
  }
}
