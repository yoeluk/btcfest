package com.btcfest

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}

object BtcfestBackend extends App {

  val config = ConfigFactory.load("backend")
  val system = ActorSystem("backend", config)

  system.actorOf(Props[BitcoinApi], "bitcoinApi")

}
