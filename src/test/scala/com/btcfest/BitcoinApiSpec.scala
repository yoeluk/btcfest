package com.btcfest

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.WordSpecLike
import org.scalatest.matchers.MustMatchers

class BitcoinApiSpec extends TestKit(ActorSystem("testTickets"))
                       with WordSpecLike
                       with MustMatchers
                       with ImplicitSender
                       with StopSystemAfterAll {
  "The BitcoinApi" must {

    "respond with with success when ask for a valid bitcoin command" in {
      import BitcoinProtocol._

      val bitcoinApi = system.actorOf(Props[BitcoinApi])
      
      bitcoinApi ! CMD(command = "getinfo", params = Nil:List[String])
      expectMsg(SuccessfulResult) //This test will fail because we don't check the content of the SuccessfulResult

      bitcoinApi ! CMD(command = "getaccountaddress", params = List("[\"myaccount\"]"))
      expectMsg(SuccessfulResult) //This test will fail because we don't check the content of the SuccessfulResult

    }
  }
}
