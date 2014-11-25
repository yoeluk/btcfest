package com.btcfest

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorIdentity
import akka.actor.Identify
import scala.language.postfixOps
import scala.concurrent.duration._

trait BitcoinApiCreator { this: Actor =>
  def createBitcoinApi: ActorRef = context.actorOf(Props[BitcoinApi], "bitcoinApi")
}

object RemoteBitcoinApiCreator {
  val config      = ConfigFactory.load("frontend").getConfig("backend")
  val host        = config.getString("host")
  val port        = config.getInt("port")
  val protocol    = config.getString("protocol")
  val systemName  = config.getString("system")
  val actorName   = config.getString("actor")
}

trait RemoteBitcoinApiCreator extends BitcoinApiCreator { this:Actor =>
  import RemoteBitcoinApiCreator._

  def createPath:String = {
    s"$protocol://$systemName@$host:$port/$actorName"
  }

  override def createBitcoinApi: ActorRef = {
    val path = createPath
    context.actorOf(
      props = Props(classOf[RemoteLookup], path), 
      name  = "lookupBitcoinApi"
    )
  }
}

class RemoteLookup(path: String)
  extends Actor with ActorLogging {

  context.setReceiveTimeout(3 seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    val selection = context.actorSelection(path)
    selection ! Identify(path)
  }

  def receive = identify

  def identify: Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("switching to active state")
      context.become(active(actor))
      context.watch(actor)

    case ActorIdentity(`path`, None) =>
      log.error(s"Remote actor with path $path is not available.")

    case ReceiveTimeout =>
      sendIdentifyRequest()

    case msg: Any =>
      log.error(s"Ignoring message $msg, remote actor is not ready yet.")
  }

  def active(actor: ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info("Actor $actorRef terminated.")
      log.info("switching to identify state")
      context.become(identify)
      context.setReceiveTimeout(3 seconds)
      sendIdentifyRequest()

    case msg:Any => actor forward msg
  }
}

trait ConfiguredRemoteBitcoinApiDeployment extends BitcoinApiCreator { this: Actor =>

  override def createBitcoinApi = {
    context.actorOf(Props[RemoteBitcoinApiForwarder], "forwarder")
  }
}

class RemoteBitcoinApiForwarder extends Actor with ActorLogging {
  context.setReceiveTimeout(3 seconds)

  deployAndWatch()

  def deployAndWatch(): Unit = {
    val actor = context.actorOf(Props[BitcoinApi], "bitcoinApi")
    context.watch(actor)
    log.info("switching to maybe active state")
    context.become(maybeActive(actor))
    context.setReceiveTimeout(Duration.Undefined)
  }

  def receive = deploying

  def deploying: Receive = {

    case ReceiveTimeout =>
      deployAndWatch()

    case msg: Any =>
      log.error(s"Ignoring message $msg, remote actor is not ready yet.")
  }

  def maybeActive(actor:ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info("Actor $actorRef terminated.")
      log.info("switching to deploying state")
      context.become(deploying)
      context.setReceiveTimeout(3 seconds)
      deployAndWatch()

    case msg: Any => actor forward msg
  }
}

trait RemoteBitcoinApiDeployment extends BitcoinApiCreator { this: Actor =>

  override def createBitcoinApi = {
    val config     = ConfigFactory.load("frontend").getConfig("backend")
    val host       = config.getString("host")
    val port       = config.getInt("port")
    val protocol   = config.getString("protocol")
    val systemName = config.getString("system")
    val actorName  = config.getString("actor")
    val path       = s"$protocol://$systemName@$host:$port/$actorName"
    val lookup     = context.system.actorOf(Props(classOf[RemoteLookup],path), "lookupBitcoinApi")
    lookup
  }
}
