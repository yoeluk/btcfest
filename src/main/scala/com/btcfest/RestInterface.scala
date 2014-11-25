package com.btcfest

import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging with BitcoinApiCreator { actor: Actor =>
  import com.btcfest.BitcoinProtocol._
  import context._
  implicit val timeout = Timeout(10 seconds)
  import akka.pattern.ask
  import akka.pattern.pipe

  val bitcoinApi = createBitcoinApi

  def routes: Route =
    path(Segment) { cmd => 
      get { requestContext =>
        val request = CMD(command = cmd, params = Nil: List[String], id = None)
        val responder = createResponder(requestContext)
        bitcoinApi.ask(request).pipeTo(responder)
      } ~
      post {
        entity(as[BtcParams]) { bp =>  requestContext =>
          val request = CMD(command = cmd, params = List(bp.params), id = bp.id)
          val responder = createResponder(requestContext)
          bitcoinApi.ask(request).pipeTo(responder)
        }
      }
    }

  def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext, bitcoinApi)))
  }
}

class Responder(requestContext:RequestContext, bitcoinApi:ActorRef) extends Actor with ActorLogging {

  import BitcoinProtocol._
  import spray.httpx.SprayJsonSupport._

  context.setReceiveTimeout(30 seconds)

  def receive = {
    case result:SuccessfulResult =>
      requestContext.complete(StatusCodes.OK, result)
      self ! PoisonPill
    case error:FailedResult =>
      requestContext.complete(StatusCodes.BadRequest, error)
      self ! PoisonPill
    case error:InternalError =>
      requestContext.complete(StatusCodes.InternalServerError, error)
      self ! PoisonPill
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      self ! PoisonPill
  }
}