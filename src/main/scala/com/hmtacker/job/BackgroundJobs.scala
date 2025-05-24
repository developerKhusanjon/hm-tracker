package com.hmtacker.job

import cats.effect.{IO, Temporal}
import cats.effect.implicits.*
import scala.concurrent.duration.*
import com.hmtacker.services.*

class BackgroundJobs(
                      taskService: TaskService,
                      notificationService: NotificationService
                    ):

  def startNotificationScheduler(): IO[Unit] =
    val job = for
      _ <- IO(println("Running notification scheduler..."))
      _ <- notificationService.sendPendingNotifications()
      _ <- IO.sleep(30.seconds) // Check every 30 seconds
    yield ()

    job.foreverM

  def startOverdueTaskChecker(): IO[Unit] =
    val job = for
      _ <- IO(println("Checking for overdue tasks..."))
      _ <- taskService.markOverdue()
      _ <- IO.sleep(5.minutes) // Check every 5 minutes
    yield ()

    job.foreverM