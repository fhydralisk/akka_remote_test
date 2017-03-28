package cn.edu.tsinghua.ee.fi.akka.remote_test

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import concurrent.duration._
import scala.util.Try

/**
  * Created by hydra on 2017/3/23.
  */
object Hbcdf {

  val defaultCountAfterTerminate = 300

  def main(args: Array[String]): Unit = {

    val host = args(1)
    val peer = args(2)
    val role = args(0)

    val config = ConfigFactory.parseString(s"""akka.remote.netty.tcp.hostname = "$host"""").withFallback(ConfigFactory.load())
    val system = ActorSystem.create("RemoteTestSystem", config)

    role match {
      case "sender" =>
        val outputPath = Try(args(3)) getOrElse "~/hbcdf.log"
        val header = Try(args(4)) getOrElse ""
        doSenderProcess(system, peer, outputPath, header)
      case "receiver" =>
        val countAfterTerminate = Try(args(3) toInt) getOrElse defaultCountAfterTerminate
        doReceiverProcess(system, countAfterTerminate)
      case _ =>
        system.terminate()
    }
  }

  def doSenderProcess(system: ActorSystem, peer: String, outputPath: String, header: String): Unit = {
    system.actorOf(Props(new HBSender(peer, outputPath, header)), name = "hbsender")
    system.actorOf(Props(new PingPongSender(peer)), name = "pingpongsender")

  }

  def doReceiverProcess(system: ActorSystem, countAfterTerminate: Int): Unit = {
    system.actorOf(Props(new HBReceiver(countAfterTerminate)), name = "hbreceiver")
    system.actorOf(Props(new PingPongReceiver), name="pingpongreceiver")
  }
}

object Messages {
  case class HeartbeatReq()
  case class HeartbeatRsp(terminate: Boolean)
}


class HBSender(peer: String, outputPath: String, header: String) extends Actor with ActorLogging {
  import Messages._
  import context.dispatcher
  implicit val timeout: Timeout = 3 seconds

  val heartbeatInterval: FiniteDuration = 1 second
  val timeoutsBeforeDown = 10
  var timeouts = 0
  var lastHeartBeat: Long = 0

  val filePrinter = new java.io.PrintWriter(new java.io.File(outputPath))

  context.system.scheduler.schedule(1 second, heartbeatInterval) {
    val receiver = context.actorSelection(s"akka.tcp://RemoteTestSystem@$peer:2551/user/hbreceiver")
    val st = System.nanoTime()
    val result = receiver ? HeartbeatReq()
    result map {
      case HeartbeatRsp(terminate) =>
        val ed = System.nanoTime()
        output(ed - st, if (lastHeartBeat == 0) -1 else ed - lastHeartBeat)
        lastHeartBeat = ed
        if (terminate)
          context.system.terminate()
        else
          timeouts = 0
    } recover {
      case _ : akka.pattern.AskTimeoutException =>
        output(-1, -1)
        timeouts += 1
        if (timeouts >= timeoutsBeforeDown)
          context.system.terminate()
      case ex: Throwable =>
        println(s"unhandled exception $ex")
    }
  }

  def receive = {
    case _ =>

  }

  def output(latency: Long, interval: Long): Unit = {
    filePrinter.println(s"$latency|$interval")
    println(s"$latency|$interval")
  }

  override def preStart(): Unit = {
    filePrinter.println(s"$header")
  }

  override def postStop(): Unit = {
    filePrinter.close()
  }
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
  val scale = 10

  context.system.scheduler.schedule(1 second, 10 millis) {
    1 to scale foreach { _ =>
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