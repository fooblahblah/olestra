package org.fooblahblah.olestra

import akka.actor._
import akka.actor.ActorDSL._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config._
import java.util.Date
import model.Model._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import Json._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.client._
import spray.http._
import spray.http.MediaTypes._
import spray.can.client.HttpClient
import spray.can.client.HttpClient._
import spray.io.IOExtension
import spray.io.SSLContextProvider
import spray.can.client.ClientSettings
import spray.http.HttpHeaders.Authorization


trait Olestra {

  implicit def system: ActorSystem

  protected lazy val logger = Logging(system, "Olestra")

  protected lazy val hostName           = "api.flowdock.com"
  protected lazy val streamingHostName  = "stream.flowdock.com"

  protected lazy val authorizationCreds = BasicHttpCredentials(apiToken, "X")

  protected def apiToken: String

  protected def flowAuthorizationCreds(flowToken: String) = BasicHttpCredentials(flowToken, "X")

  protected def client: HttpRequest => Future[HttpResponse]

  protected def streamingClient: ActorRef


  def GET(uri: String) = client(HttpRequest(method = HttpMethods.GET, uri = uri))

  def POST(uri: String, body: HttpEntity = EmptyEntity) = client(HttpRequest(method = HttpMethods.POST, uri = uri, entity = body))

  def PUT(uri: String, body: HttpEntity = EmptyEntity) = client(HttpRequest(method = HttpMethods.PUT, uri = uri, entity = body))


  def organizations: Future[List[Organization]] = GET("/organizations") map { response =>
    response.status match {
      case StatusCodes.OK => parse(response.entity.asString).as[List[Organization]]
      case _              => Nil
    }
  }

  def organization(orgId: String): Future[Option[Organization]] = GET("s/organizations/${orgId}") map { response =>
    response.status match {
      case StatusCodes.OK => Some(parse(response.entity.asString).as[Organization])
      case _              => None
    }
  }

  def flows: Future[List[Flow]] = GET("/flows") map { response =>
    response.status match {
      case StatusCodes.OK => parse(response.entity.asString).as[List[Flow]]
      case _              => Nil
    }
  }


  def flow(id: String): Future[Option[Flow]] = GET(s"/flows/${id}") map { response =>
    response.status match {
      case StatusCodes.OK => Some((parse(response.entity.asString) \ "room").as[Flow])
      case _              => None
    }
  }


  def messages(flowId: String): Future[List[Message]] = GET(s"/flows/${flowId}/messages") map { response =>
    response.status match {
      case StatusCodes.OK => parse(response.entity.asString).as[List[Message]]
      case _              => Nil
    }
  }


  def users: Future[List[User]] = organizations map { orgs =>
    (orgs flatMap { org =>
      org.users
    } toSet) toList
  }


  def usersForFlow(flowId: String): Future[List[User]] = GET(s"/flows/${flowId}/users") map { response =>
    response.status match {
      case StatusCodes.OK => parse(response.entity.asString).as[List[User]]
      case _              => Nil
    }
  }

  def usersForOrganization(orgId: String): Future[List[User]] = organization(orgId) map { f =>
    f.map(_.users).getOrElse(Nil)
  }


  def sendMessage(flowId: String, message: String): Future[Boolean] = {
    val body = HttpBody(ContentType(`application/json`), Json.obj("event" -> "message", "content" -> message, "tags" -> JsArray(Nil)).toString)
    POST(s"/flows/${flowId}/messages", body) map { response =>
      response.status == StatusCodes.OK
    }
  }

  def sendMessage(flowToken: String, user: String, message: String): Future[Boolean] = {
    val body    = HttpBody(ContentType(`application/json`), Json.obj("external_user_name" -> user, "content" -> message).toString)
    val request = HttpRequest(method = HttpMethods.POST, uri = s"/v1/messages/chat/${flowToken}", entity = body)

    client(request) map { response =>
      response.status == StatusCodes.OK
    }
  }

  def live(flowId: String, fn: (Message) => Unit): ActorRef = {

    class Streamer extends Actor {
      val uri = s"/flows/${flowId}"

      var retrying: Boolean = false

      def reconnect() {
        retrying = true
        logger.info("Retrying connection in 3s ...")
        system.scheduler.scheduleOnce(3 seconds, self, Connect)
      }

      def receive = {
        case Connect =>
          logger.info(s"Attempting to connect: $streamingHostName$uri")
          streamingClient ! Connect(streamingHostName, 443, HttpClient.SslEnabled)
          sender ! true

        case Status.Failure(reason) =>
          logger.info(s"Failed to connect: ${reason}")
          reconnect()

        case Connected(connection) =>
          logger.info(s"Connected to stream: $streamingHostName$uri")
          retrying = false
          val request = HttpRequest(method = HttpMethods.GET, uri = uri, headers = Authorization(authorizationCreds) :: Nil)
          connection.handler ! request

        case m: MessageChunk if(!m.bodyAsString.trim.isEmpty) =>
          m.bodyAsString.trim split(13.toChar) map { json =>
            fn(parse(json).as[Message])
          }

        case MessageChunk(_, _) =>

        case Closed(reason) =>
          logger.info(s"Connection closed: ${reason}")
          if(!retrying) reconnect()

        case m =>
          logger.info(s"${m}")
      }
    }

    val streamer = system.actorOf(Props(new Streamer))
    streamer ! Connect

    streamer
  }
}


object Olestra {

  import HttpConduit._
  import SSLContextProvider._
  import scala.util.control.Exception._


  def apply()(implicit system: ActorSystem): Olestra = {
    val config           = ConfigFactory.load
    val reconnectTimeout = failAsValue(classOf[Exception])(config.getInt("reconnect-timeout"))(5)

    apply(config.getString("token"), reconnectTimeout)
  }

  def apply(token: String, reconnectTimeout: Int = 5)(implicit system_ : ActorSystem): Olestra = new Olestra {

    val system = system_
    val apiToken = token

    val ioBridge = IOExtension(system).ioBridge
    val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))

    val conduit = system.actorOf(Props(new HttpConduit(httpClient, hostName, 443, true)))

    val client =
      addCredentials(authorizationCreds) ~>
      sendReceive(conduit)

    val clientConfig = """
      spray.can.client.ssl-encryption = on
      spray.can.client.response-chunk-aggregation-limit = 0
      spray.can.client.idle-timeout = 300s
      spray.can.client.request-timeout = 0s
      """
    val settings = ClientSettings(ConfigFactory.parseString(clientConfig))
    val streamingClient = system.actorOf(Props(new HttpClient(ioBridge, settings)))
  }
}
