package cn.edu.tsinghua.ee.fi.akka.remote_test

import akka.actor.{Actor, ActorLogging, Props}

/**
  * Created by hydra on 2017/3/1.
  */

object Receiver {
  def props: Props = Props(classOf[Receiver])
}


class Receiver extends Actor with ActorLogging {

  override def receive = {
    case msg : String =>
      log.info(s"Message received $msg")
  }
}
