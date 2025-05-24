package com.hmtacker.repos.postgres

import cats.effect.IO
import cats.implicits.*
import com.hmtacker.domain.*
import com.hmtacker.repos.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

import java.time.LocalDateTime
import java.util.UUID

class PostgresNotificationRepository(xa: Transactor[IO]) extends NotificationRepository:

  def create(notification: Notification): IO[Notification] =
    sql"""
      INSERT INTO notifications (id, user_id, task_id, title, body, scheduled_at, sent_at, delivered)
      VALUES (${notification.id}, ${notification.userId}, ${notification.taskId}, 
              ${notification.title}, ${notification.body}, ${notification.scheduledAt},
              ${notification.sentAt}, ${notification.delivered})
    """.update.run.transact(xa) *> IO.pure(notification)

  def findPendingNotifications(): IO[List[Notification]] =
    sql"""
      SELECT id, user_id, task_id, title, body, scheduled_at, sent_at, delivered
      FROM notifications 
      WHERE sent_at IS NULL AND scheduled_at <= ${LocalDateTime.now()}
      ORDER BY scheduled_at
    """.query[Notification].to[List].transact(xa)

  def markAsSent(id: UUID): IO[Unit] =
    sql"""
      UPDATE notifications 
      SET sent_at = ${LocalDateTime.now()}
      WHERE id = $id
    """.update.run.transact(xa).void

  def markAsDelivered(id: UUID): IO[Unit] =
    sql"""
      UPDATE notifications 
      SET delivered = TRUE
      WHERE id = $id
    """.update.run.transact(xa).void