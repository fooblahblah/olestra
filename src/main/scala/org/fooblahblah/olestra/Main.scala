package org.fooblahblah.olestra

import model.Model._
import akka.actor._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TestApp {
  implicit val system = ActorSystem()

  val client = Olestra()

  def main(args: Array[String]) {
    import client._

    val flowId = "victorops/main"

//    organizations map { orgs =>
//      orgs foreach { org =>
//        println(org.users)
//      }
//    } foreach { i =>
//      sys.exit
//    }

//      Future(live(flowId, (msg: Message) => if(msg.content.isDefined) println(msg)))

  }
}
