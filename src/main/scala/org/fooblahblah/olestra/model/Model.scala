package org.fooblahblah.olestra.model

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import play.api.libs.functional.syntax._


object Model {

  case class Organization(id: String, name: String, userLimit: Int, userCount: Int, isActive: Boolean, users: List[User])

  case class User(id: Int, name: String, email: String, nick: String, avatarUrl: String)

  case class Flow(id: String, name: String, organization: String, unreadMentions: Int, isOpen: Boolean, isJoined: Boolean, url: String, webUrl: String, joinUrl: String, accessMode: String, users: Option[List[User]] = None)

  // content is more elaborate than just strings, but for now that's all we handle
  case class Message(id: Int, event: String, content: Option[String], userId: Int, flowId: String, sent: Date, tags: List[String]) //, attachments: List[Attachment])

  case class Attachment(id: String, fileName: String, contentType: String, size: Long, path: String)

  case class Speak(message: String) {
    def toJSON = Json.obj("message" -> Json.obj("body" -> JsString(message)))
  }


  implicit val userReads: Reads[User] = (
    (__ \ "id").read[Int] ~
    (__ \ "name").read[String] ~
    (__ \ "email").read[String] ~
    (__ \ "nick").read[String] ~
    (__ \ "avatar").read[String])(User)


  implicit val organizationReads: Reads[Organization] = (
    (__ \ "id").read[String] ~
    (__ \ "name").read[String] ~
    (__ \ "user_limit").read[Int] ~
    (__ \ "user_count").read[Int] ~
    (__ \ "active").read[Boolean] ~
    (__ \ "users").read(list[User]))(Organization)


  implicit val flowReads: Reads[Flow] = (
    (__ \ "id").read[String] ~
    (__ \ "name").read[String] ~
    (__ \ "organization").read[String] ~
    (__ \ "unread_mentions").read[Int] ~
    (__ \ "open").read[Boolean] ~
    (__ \ "joined").read[Boolean] ~
    (__ \ "url").read[String] ~
    (__ \ "web_url").read[String] ~
    (__ \ "join_url").read[String] ~
    (__ \ "access_mode").read[String] ~
    (__ \ "users").readNullable(list[User]))(Flow)


  implicit val messageReads: Reads[Message] = (
    (__ \ "id").read[Int] ~
    (__ \ "event").read[String] ~
    optionNoError((__ \ "content").read[String]) ~
    (__ \ "user").read[String].map(_.toInt).or((__ \ "user").read[Int]) ~
    (__ \ "flow").read[String] ~
    (__ \ "sent").read[Long].map(new Date(_)) ~
    (__ \ "tags").read(list[String]))(Message)


  implicit val attachmentReads: Reads[Attachment] = (
    (__ \ "id").read[String] ~
    (__ \ "file_name").read[String] ~
    (__ \ "content_type").read[String] ~
    (__ \ "file_size").read[Long] ~
    (__ \ "path").read[String])(Attachment)

}
