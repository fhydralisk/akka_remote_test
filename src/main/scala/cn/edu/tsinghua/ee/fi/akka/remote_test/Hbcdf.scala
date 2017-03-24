package cn.edu.tsinghua.ee.fi.akka.remote_test

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

import concurrent.duration._

/**
  * Created by hydra on 2017/3/23.
  */
object Hbcdf {

  val heartbeatInterval: FiniteDuration = 1 second
  def main(args: Array[String]): Unit = {

    val host = args(1)
    val peer = args(2)
    val role = args(0)

    val config = ConfigFactory.parseString(s"""akka.remote.netty.tcp.hostname = "$host"""").withFallback(ConfigFactory.load())
    val system = ActorSystem.create("RemoteTestSystem", config)

    role match {
      case "sender" =>
        doSenderProcess(system, peer)
      case "receiver" =>
        doReceiverProcess(system)
      case _ =>
        system.terminate()
    }
  }

  def doSenderProcess(system: ActorSystem, peer: String): Unit = {
    import system.dispatcher
    import akka.util.Timeout
    implicit val timeout: Timeout = 50 millis
    import Messages._

    system.scheduler.schedule(1 second, heartbeatInterval) {
      val receiver = system.actorSelection(s"akka.tcp://RemoteTestSystem@$peer:2551/user/hbreceiver")
      val st = System.nanoTime()
      val result = receiver ? HeartbeatReq()
      result map {
        case HeartbeatRsp(terminate) =>
          val ed = System.nanoTime()
          println(ed - st)
          if (terminate)
            system.terminate()
      } recover {
        case _ : akka.pattern.AskTimeoutException =>
          println("timeout")
        case ex: Throwable =>
          println(s"unhandled exception $ex")
      }
    }
  }

  def doReceiverProcess(system: ActorSystem): Unit = {
    system.actorOf(Props(new HBReceiver(100)), name = "hbreceiver")
  }
}

object Messages {
  case class HeartbeatReq()
  case class HeartbeatRsp(terminate: Boolean)
}


class HBReceiver(count: Int) extends Actor with ActorLogging {

  var counter = 0

  import Messages._

  def receive = {
    case _ : HeartbeatReq =>
      counter += 1
      sender() ! HeartbeatRsp(counter >= count)
      log.info("heartbeat req")
      if (counter >= count)
        context.system.terminate()
    case _ =>
      // Do nothing
  }

}