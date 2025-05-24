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

// Custom Meta instances for enums
given Meta[TaskState] = pgEnumString("task_state", TaskState.valueOf, _.toString)
given Meta[Priority] = pgEnumString("priority", Priority.valueOf, _.toString)
given Meta[Subject] = pgEnumString("subject", Subject.valueOf, _.toString)
given Meta[UserId] = Meta[UUID].timap(UserId.apply)(_.value)
given Meta[TaskId] = Meta[UUID].timap(TaskId.apply)(_.value)
given Meta[CourseId] = Meta[UUID].timap(CourseId.apply)(_.value)

class PostgresUserRepository(xa: Transactor[IO]) extends UserRepository:

  def create(user: User): IO[User] =
    sql"""
      INSERT INTO users (id, email, first_name, last_name, fcm_token, created_at, updated_at)
      VALUES (${user.id}, ${user.email}, ${user.firstName}, ${user.lastName},
              ${user.fcmToken}, ${user.createdAt}, ${user.updatedAt})
    """.update.run.transact(xa) *> IO.pure(user)

  def findById(id: UserId): IO[Option[User]] =
    sql"""
      SELECT id, email, first_name, last_name, fcm_token, created_at, updated_at
      FROM users WHERE id = $id
    """.query[User].option.transact(xa)

  def findByEmail(email: String): IO[Option[User]] =
    sql"""
      SELECT id, email, first_name, last_name, fcm_token, created_at, updated_at
      FROM users WHERE email = $email
    """.query[User].option.transact(xa)

  def update(user: User): IO[User] =
    sql"""
      UPDATE users
      SET email = ${user.email}, first_name = ${user.firstName},
          last_name = ${user.lastName}, fcm_token = ${user.fcmToken},
          updated_at = ${user.updatedAt}
      WHERE id = ${user.id}
    """.update.run.transact(xa) *> IO.pure(user)

  def updateFcmToken(userId: UserId, token: String): IO[Unit] =
    sql"""
      UPDATE users SET fcm_token = $token, updated_at = ${LocalDateTime.now()}
      WHERE id = $userId
    """.update.run.transact(xa).void
