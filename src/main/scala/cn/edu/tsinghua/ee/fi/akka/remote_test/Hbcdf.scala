package cn.edu.tsinghua.ee.fi.akka.remote_test

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
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
    implicit val timeout: Timeout = 3000 millis
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
    system.scheduler.scheduleOnce(1 second) {
      system.actorOf(Props(new PingPongSender(peer)))
    }
  }

  def doReceiverProcess(system: ActorSystem): Unit = {
    system.actorOf(Props(new HBReceiver(100)), name = "hbreceiver")
    system.actorOf(Props(new PingPongReceiver), name="pingpongreceiver")
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

/**
  * Together with @PingPongSender, simulates user logic
  */

class PingPongSender(peer: String) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  import context.dispatcher

  context.system.scheduler.schedule(0 second, 10 millis) {
    1 to 1000 foreach { _ =>
      context.actorSelection(s"akka.tcp://RemoteTestSystem@$peer:2551/user/pingpongreceiver") ! "Hello World"
    }
  }

  def receive = {
    case _ =>
      // do nothing
  }
}

class PingPongReceiver extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {
  def receive = {
    case s @ _ =>
      sender() ! s
  }
}