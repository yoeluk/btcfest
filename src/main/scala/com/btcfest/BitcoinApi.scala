package com.btcfest

import akka.actor._
import concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps
import spray.http._
import HttpMethods._
import ContentTypes._
import scala.util.{Success, Failure}
import HttpCharsets._

import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global

class BitcoinApi extends Actor with ActorLogging {
  import BitcoinProtocol._
  implicit val timeout = Timeout(5 seconds)
  
  val rnd = new scala.util.Random
  def randIdWithLength(alphabet: String = "0123456789")(n: Int): String = 
    Stream.continually(rnd.nextInt(alphabet.size)).map(alphabet).take(n).mkString
  def randId = randIdWithLength()(6)
  
  val config   = ConfigFactory.load("backend")
  val user     = config.getString("bitcoind.user")
  val secret   = config.getString("bitcoind.secret")
  val host     = config.getString("bitcoind.host")
  val port     = config.getString("bitcoind.port")
  val baseUri  = s"http://$host:$port/"

  def receive = {
    
    case request: CMD =>
      import spray.client.pipelining._
      import spray.httpx.SprayJsonSupport._
      import spray.util._
      
      val captSender  = sender
      val method      = request.command
      val reqId       = request.id match {
        case None     => s"btcfest-$randId"
        case Some(id) => id
      }
      val params = request.params match {
        case Nil => "[]"
        case p   => p.head
      }
      val payload = s"""|{
                        | "jsonrpc" : "1.0",
                        | "id"      : "$reqId",
                        | "method"  : "$method",
                        | "params"  : $params
                        |}""".stripMarginWithNewline("\n")
      
      val pipeline: HttpRequest => Future[BtcResponse] = (
        addCredentials(BasicHttpCredentials(user, secret)) 
        ~> sendReceive
        ~> unmarshal[BtcResponse]
      )
      val btcResponse = pipeline(
        HttpRequest(
          method = POST, 
          uri    = baseUri, 
          entity = HttpEntity(`text/plain`, payload)
        )
      )
      
      btcResponse onComplete {
        case Success(response) =>
          response.result match {
            case Some(result) =>
              captSender ! SuccessfulResult(result = result, id = response.id.get)
            case None =>
              captSender ! FailedResult(error = response.error.get, id = response.id.get)
          }
        case Failure(e)  => captSender ! InternalError(errorMsg = e.getMessage)
        case _           => captSender ! InternalError(errorMsg = "error")
      }

  }

}
