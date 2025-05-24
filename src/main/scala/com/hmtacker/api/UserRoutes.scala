package com.hmtacker.api

import cats.effect.IO
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import java.time.LocalDateTime
import java.util.UUID
import com.hmtacker.domain.*
import com.hmtacker.repos.*

class UserRoutes(userRepo: UserRepository):

  case class CreateUserRequest(
                                email: String,
                                firstName: String,
                                lastName: String,
                                fcmToken: Option[String]
                              )

  case class UpdateFcmTokenRequest(fcmToken: String)

  case class UserResponse(
                           id: String,
                           email: String,
                           firstName: String,
                           lastName: String,
                           createdAt: String
                         )

  object UserResponse:
    def fromDomain(user: User): UserResponse =
      UserResponse(
        id = user.id.value.toString,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        createdAt = user.createdAt.toString
      )

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / "users" =>
      for
        createReq <- req.as[CreateUserRequest]
        now = LocalDateTime.now()
        user = User(
          id = UserId(UUID.randomUUID()),
          email = createReq.email,
          firstName = createReq.firstName,
          lastName = createReq.lastName,
          fcmToken = createReq.fcmToken,
          createdAt = now,
          updatedAt = now
        )
        createdUser <- userRepo.create(user)
        response <- Ok(UserResponse.fromDomain(createdUser).asJson)
      yield response

    case GET -> Root / "users" / UUIDVar(userId) =>
      userRepo.findById(UserId(userId)).flatMap {
        case Some(user) => Ok(UserResponse.fromDomain(user).asJson)
        case None => NotFound("User not found")
      }

    case req @ PUT -> Root / "users" / UUIDVar(userId) / "fcm-token" =>
      for
        updateReq <- req.as[UpdateFcmTokenRequest]
        _ <- userRepo.updateFcmToken(UserId(userId), updateReq.fcmToken)
        response <- Ok("FCM token updated successfully")
      yield response
  }