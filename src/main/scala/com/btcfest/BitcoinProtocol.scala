package com.btcfest

object BitcoinProtocol {
  import spray.json._
  
  case class CMD(command: String, params: List[String], id: Option[String])
  
  case class BtcParams(params: String, id: Option[String])
  
  case class BtcInfo(info: String)
  
  case class BtcResponse(result: Option[JsValue], error: Option[JsValue], id: Option[String])
  
  case class SuccessfulResult(result: JsValue, id: String)
  
  case class FailedResult(error: JsValue, id: String)
  
  case class InternalError(errorMsg: String)

  //----------------------------------------------
  // JSON
  //----------------------------------------------

  object BtcInfo extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(BtcInfo.apply)
  }
  
  object BtcResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(BtcResponse.apply)
  }
  
  object SuccessfulResult extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(SuccessfulResult.apply)
  }
  
  object FailedResult extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FailedResult.apply)
  }
  
  object BtcParams extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(BtcParams.apply)
  }
  
  object InternalError extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(InternalError.apply)
  }

}