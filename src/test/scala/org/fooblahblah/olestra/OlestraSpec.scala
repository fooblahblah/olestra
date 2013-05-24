package org.fooblahblah.olestra

import akka.actor._
import java.util.concurrent.TimeUnit
import model.Model._
import org.junit.runner._
import org.specs2.matcher.Matchers._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.time.TimeConversions._
import play.api.libs.json._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http._

@RunWith(classOf[JUnitRunner])
class OlestraSpec extends Specification with Olestra {

  val system = ActorSystem()

  sequential

  val apiToken = "123456"
  val flowId   = "victorops/main"

  val streamingClient = system.actorOf(Props[MockStreamingActor])


  "Olestra" should {
    "Form Authorization header with token" in {
      val h = authorizationCreds
      h.toString must startWith("Basic ")
    }

    "Support organizations" in {
      val result = Await.result(organizations, Duration(1, TimeUnit.SECONDS))
      result must beAnInstanceOf[List[Organization]]
      result.map(_.id) === List("victorops")
    }

    "Support users for an organization" in {
      val result = Await.result(users, Duration(1, TimeUnit.SECONDS))
      result.map(_.id) === List(38440, 38441, 38442)
    }

    "Support users for a flow" in {
      val result = Await.result(usersForFlow(flowId), Duration(1, TimeUnit.SECONDS))
      result.map(_.id) === List(38440, 38441, 38442)
    }

    "Support messages for a flow" in {
      val result = Await.result(messages(flowId), Duration(1, TimeUnit.SECONDS))
      result.map(_.id) === List(158, 179, 846, 850)
    }
  }


  val client = { request: HttpRequest =>
    import HttpMethods._

    (request.method, request.uri) match {
      case (GET,  "/organizations")                 => Future.successful(HttpResponse(status = StatusCodes.OK, entity = HttpBody(organizationsArtifact)))
      case (GET,  "/flows")                         => Future.successful(HttpResponse(status = StatusCodes.OK, entity = HttpBody(flowsArtifact)))
      case (GET,  "/flows/victorops/main/messages") => Future.successful(HttpResponse(status = StatusCodes.OK, entity = HttpBody(messagesArtifact)))
      case (GET,  "/flows/victorops/main/users")    => Future.successful(HttpResponse(status = StatusCodes.OK, entity = HttpBody(usersArtifact)))
      case _                                        => Future.successful(HttpResponse(status = StatusCodes.NotFound))
    }
  }

  val organizationsArtifact = """
    [
      {
        "_links": {
          "subscription_management": {
            "href": "https://victorops.flowdock.com/subscriptions/new"
          },
          "account_management": {
            "href": "https://victorops.flowdock.com/"
          },
          "flows": {
            "href": "https://api.flowdock.com/flows/victorops",
            "methods": [
              "GET",
              "POST"
            ]
          }
        },
        "id": "victorops",
        "name": "VictorOps",
        "user_limit": 80,
        "user_count": 3,
        "active": true,
        "subscription": {
          "trial": true,
          "trial_ends": "2013-06-21"
        },
        "users": [
          {
            "id": 38440,
            "name": "Jeff Simpson",
            "email": "jeff@victorops.com",
            "admin": true,
            "nick": "Jeff",
            "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/local/589d8824b8e9343ecdc54fe4623445fe7e0dd01bcc2c70ce444b0098f96f8d17/"
          },
          {
            "id": 38441,
            "name": "Dan Hopkins",
            "email": "danhopkins@victorops.com",
            "admin": false,
            "nick": "Dan",
            "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/6b919340e79c40b3bfb57b44bdf21288/"
          },
          {
            "id": 38442,
            "name": "Todd Vernon",
            "email": "todd@victorops.com",
            "admin": false,
            "nick": "Todd",
            "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/4703106fbcc2c23cd95c00bb83e14e12/"
          }
        ]
      }
    ]
    """

  val flowArtifact = """
    {
      "name": "blah",
      "email": "blah@victorops.flowdock.com",
      "id": "victorops/blah",
      "api_token": "c5c8d46757658b25ee2b07150befb1be",
      "access_mode": "invitation",
      "organization": "VictorOps",
      "url": "https://api.flowdock.com/flows/victorops/blah",
      "web_url": "https://www.flowdock.com/app/victorops/blah",
      "unread_mentions": 0,
      "open": false,
      "joined": true,
      "users": [
        {
          "id": 38440,
          "nick": "Jeff",
          "name": "Jeff Simpson",
          "email": "jeff@victorops.com",
          "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/local/589d8824b8e9343ecdc54fe4623445fe7e0dd01bcc2c70ce444b0098f96f8d17/",
          "status": null,
          "disabled": false,
          "last_activity": 1369242706608,
          "last_ping": 1369242667509
        }
      ]
    }
  """

  val flowsArtifact = """
    [
      {
        "name": "Main",
        "email": "main@victorops.flowdock.com",
        "id": "victorops/main",
        "api_token": "a81e021efd900a650bc78c7ebc6d9663",
        "access_mode": "invitation",
        "organization": "VictorOps",
        "url": "https://api.flowdock.com/flows/victorops/main",
        "web_url": "https://www.flowdock.com/app/victorops/main",
        "unread_mentions": 0,
        "open": true,
        "joined": true,
        "join_url": "https://victorops.flowdock.com/invitations/83cc7a15c6a408fe49cfd3dde2387b9880661ab6-main"
      },
      {
        "name": "blah",
        "email": "blah@victorops.flowdock.com",
        "id": "victorops/blah",
        "api_token": "c5c8d46757658b25ee2b07150befb1be",
        "access_mode": "invitation",
        "organization": "VictorOps",
        "url": "https://api.flowdock.com/flows/victorops/blah",
        "web_url": "https://www.flowdock.com/app/victorops/blah",
        "unread_mentions": 0,
        "open": false,
        "joined": true
      }
    ]
  """

  val usersArtifact = """
    [
      {
        "id": 38440,
        "nick": "Jeff",
        "name": "Jeff Simpson",
        "email": "jeff@victorops.com",
        "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/local/589d8824b8e9343ecdc54fe4623445fe7e0dd01bcc2c70ce444b0098f96f8d17/",
        "status": null,
        "disabled": false,
        "last_activity": 1369326359424,
        "last_ping": 1369326667417
      },
      {
        "id": 38441,
        "nick": "Dan",
        "name": "Dan Hopkins",
        "email": "danhopkins@victorops.com",
        "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/6b919340e79c40b3bfb57b44bdf21288/",
        "status": "blow away",
        "disabled": false,
        "last_activity": 1369319201252,
        "last_ping": 1369319198053
      },
      {
        "id": 38442,
        "nick": "Todd",
        "name": "Todd Vernon",
        "email": "todd@victorops.com",
        "avatar": "https://d2cxspbh1aoie1.cloudfront.net/avatars/4703106fbcc2c23cd95c00bb83e14e12/",
        "status": null,
        "disabled": false,
        "last_activity": 1369323680177,
        "last_ping": 1369326678210
      }
    ]
  """

  val messagesArtifact = """
    [
     {
        "user": 38440,
        "content": {
          "path": "/flows/victorops/main/files/sJjWa5IFVjUhGpic_ksHew/sd.svg",
          "file_name": "sd.svg",
          "image": {
            "width": 3921,
            "height": 2490
          },
          "file_size": 71895,
          "content_type": "image/svg+xml",
          "thumbnail": {
            "width": 100,
            "height": 64,
            "path": "/flows/victorops/main/files/sJjWa5IFVjUhGpic_ksHew/thumb/sd.svg"
          }
        },
        "event": "file",
        "tags": [
          ":file"
        ],
        "id": 158,
        "sent": 1369242814479,
        "edited": null,
        "attachments": [
          {
            "path": "/flows/victorops/main/files/sJjWa5IFVjUhGpic_ksHew/sd.svg",
            "file_name": "sd.svg",
            "image": {
              "width": 3921,
              "height": 2490
            },
            "file_size": 71895,
            "content_type": "image/svg+xml",
            "thumbnail": {
              "path": "/flows/victorops/main/files/sJjWa5IFVjUhGpic_ksHew/thumb/sd.svg",
              "width": 100,
              "height": 64
            }
          }
        ],
        "uuid": "iEbMJyr3Ili20M9J",
        "app": "chat",
        "flow": "victorops:main"
      },
      {
        "user": 38440,
        "content": "https://github.com/fooblahblah/bivouac",
        "event": "message",
        "tags": [
          ":url"
        ],
        "id": 179,
        "sent": 1369243312410,
        "edited": null,
        "attachments": [

        ],
        "uuid": "JNIZGWBMkB2gWzoK",
        "app": "chat",
        "flow": "victorops:main"
      },
      {
        "user": 38440,
        "content": "yeay booooy",
        "event": "message",
        "tags": [

        ],
        "id": 846,
        "sent": 1369319002710,
        "edited": null,
        "attachments": [

        ],
        "uuid": "rl_5-dgMfoSkUYbN",
        "app": "chat",
        "flow": "victorops:main"
      },
      {
        "user": 0,
        "content": "Hello from curl",
        "event": "message",
        "tags": [

        ],
        "id": 850,
        "sent": 1369319178321,
        "edited": null,
        "attachments": [

        ],
        "app": "chat",
        "flow": "victorops:main",
        "external_user_name": "victorbot"
      }
    ]
  """
}

class MockStreamingActor extends Actor {
  def receive = {
    case _ => sys.error("mock streaming not implemented")
  }
}
