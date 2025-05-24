package com.hmtacker

import cats.effect.{IO, IOApp, Resource}
import cats.effect.std.Supervisor
import cats.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.server.Router
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import com.comcast.ip4s.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.hmtacker.config.*
import com.hmtacker.services.*
import com.hmtacker.repos.*
import com.hmtacker.api.*
import com.hmtacker.job.*

object HomeTaskTrackerApp extends IOApp.Simple:

  private def createTransactor(config: DatabaseConfig): Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](config.maxPoolSize)
      xa <- HikariTransactor.newHikariTransactor[IO](
        config.driver,
        config.url,
        config.username,
        config.password,
        ce
      )
    yield xa

  private def initializeFirebase(): IO[Unit] =
    IO {
      if (FirebaseApp.getApps().isEmpty) {
        // In production, use service account key file
        val options = FirebaseOptions.builder()
          .setCredentials(com.google.auth.oauth2.GoogleCredentials.getApplicationDefault())
          .build()
        FirebaseApp.initializeApp(options)
      }
    }.handleErrorWith(error =>
      IO(println(s"Firebase initialization failed: ${error.getMessage}")) *>
        IO(println("Continuing without Firebase - notifications will be logged only"))
    )

  private def createServices(xa: HikariTransactor[IO]) = {
    val userRepo = new PostgresUserRepository(xa)
    val courseRepo = new CourseRepositoryImpl(xa)
    val taskRepo = new TaskRepositoryImpl(xa)
    val notificationRepo = new NotificationRepositoryImpl(xa)
    val transitionRepo = new TaskStateTransitionRepositoryImpl(xa)

    val fcmService = new FcmService()
    val notificationService = new NotificationService(notificationRepo, fcmService, userRepo)
    val taskService = new TaskService(taskRepo, transitionRepo, notificationService)

    (taskService, notificationService, userRepo, courseRepo)
  }

  private def createHttpApp(taskService: TaskService, userRepo: UserRepository, courseRepo: CourseRepository) = {
    val taskRoutes = new TaskRoutes(taskService)
    val userRoutes = new UserRoutes(userRepo)
    val courseRoutes = new CourseRoutes(courseRepo)

    val httpApp = Router(
      "/api/v1" -> (taskRoutes.routes <+> userRoutes.routes <+> courseRoutes.routes)
    ).orNotFound

    // Add CORS and logging middleware
    Logger.httpApp(true, true)(CORS.policy.withAllowOriginAll(httpApp))
  }

  def run: IO[Unit] = {
    val config = AppConfig()

    createTransactor(config.database).use { xa =>
      for
        _ <- IO(println("Starting Academic Task Manager..."))
        _ <- initializeFirebase()

        (taskService, notificationService, userRepo, courseRepo) = createServices(xa)
        httpApp = createHttpApp(taskService, userRepo, courseRepo)

        // Start background jobs
        _ <- Supervisor[IO].use { supervisor =>
          val jobs = new BackgroundJobs(taskService, notificationService)

          for
            _ <- supervisor.supervise(jobs.startNotificationScheduler())
            _ <- supervisor.supervise(jobs.startOverdueTaskChecker())

            // Start HTTP server
            _ <- EmberServerBuilder.default[IO]
              .withHost(Host.fromString(config.server.host).get)
              .withPort(Port.fromInt(config.server.port).get)
              .withHttpApp(httpApp)
              .build
              .use { server =>
                IO(println(s"Server started at http://${config.server.host}:${config.server.port}")) *>
                  IO.never
              }
          yield ()
        }
      yield ()
    }
  }