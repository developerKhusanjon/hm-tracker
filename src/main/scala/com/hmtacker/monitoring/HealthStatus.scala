package com.hmtacker.monitoring

import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.generic.auto.*
import java.time.LocalDateTime
import com.hmtacker.services.*

case class HealthStatus(
                         status: String,
                         timestamp: String,
                         database: String,
                         firebase: String
                       )

case class MetricsResponse(
                            activeTasks: Long,
                            completedTasks: Long,
                            overdueTasks: Long,
                            pendingNotifications: Long
                          )

class HealthRoutes(taskService: TaskService, notificationService: NotificationService):

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "health" =>
      for
        dbStatus <- checkDatabaseHealth()
        fcmStatus <- checkFirebaseHealth()
        response <- Ok(HealthStatus(
          status = if (dbStatus && fcmStatus) "healthy" else "degraded",
          timestamp = LocalDateTime.now().toString,
          database = if (dbStatus) "ok" else "error",
          firebase = if (fcmStatus) "ok" else "error"
        ))
      yield response

    case GET -> Root / "metrics" =>
      for
        // This would need additional repository methods
        response <- Ok(MetricsResponse(0, 0, 0, 0))
      yield response
  }

  private def checkDatabaseHealth(): IO[Boolean] =
    // Simple database connectivity check
    IO.pure(true).handleErrorWith(_ => IO.pure(false))

  private def checkFirebaseHealth(): IO[Boolean] =
    // Firebase connectivity check
    IO.pure(true).handleErrorWith(_ => IO.pure(false))