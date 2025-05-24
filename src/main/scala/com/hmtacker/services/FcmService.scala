package com.hmtacker.services

import cats.effect.IO

class FcmService:
  import com.google.firebase.messaging._
  import com.google.firebase.FirebaseApp
  import scala.jdk.CollectionConverters.*

  def sendNotification(fcmToken: String, title: String, body: String): IO[Unit] =
    IO {
      val message = Message.builder()
        .setToken(fcmToken)
        .setNotification(
          Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()
        )
        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
        .build()

      FirebaseMessaging.getInstance().send(message)
    }.void.handleErrorWith(error =>
      IO(println(s"Failed to send notification: ${error.getMessage}"))
    )

  def sendBatchNotifications(tokens: List[String], title: String, body: String): IO[Unit] =
    IO {
      val message = MulticastMessage.builder()
        .addAllTokens(tokens.asJava)
        .setNotification(
          Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()
        )
        .build()

      FirebaseMessaging.getInstance().sendMulticast(message)
    }.void.handleErrorWith(error =>
      IO(println(s"Failed to send batch notifications: ${error.getMessage}"))
    )