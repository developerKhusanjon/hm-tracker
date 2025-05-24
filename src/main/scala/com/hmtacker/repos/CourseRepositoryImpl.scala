package com.hmtacker.repos

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

class CourseRepositoryImpl(xa: Transactor[IO]) extends CourseRepository:

  def create(course: Course): IO[Course] =
    sql"""
      INSERT INTO courses (id, user_id, name, subject, instructor, description, created_at)
      VALUES (${course.id}, ${course.userId}, ${course.name}, ${course.subject},
              ${course.instructor}, ${course.description}, ${course.createdAt})
    """.update.run.transact(xa) *> IO.pure(course)

  def findById(id: CourseId): IO[Option[Course]] =
    sql"""
      SELECT id, user_id, name, subject, instructor, description, created_at
      FROM courses WHERE id = $id
    """.query[Course].option.transact(xa)

  def findByUserId(userId: UserId): IO[List[Course]] =
    sql"""
      SELECT id, user_id, name, subject, instructor, description, created_at
      FROM courses WHERE user_id = $userId ORDER BY name
    """.query[Course].to[List].transact(xa)

  def update(course: Course): IO[Course] =
    sql"""
      UPDATE courses 
      SET name = ${course.name}, subject = ${course.subject},
          instructor = ${course.instructor}, description = ${course.description}
      WHERE id = ${course.id}
    """.update.run.transact(xa) *> IO.pure(course)

  def delete(id: CourseId): IO[Unit] =
    sql"DELETE FROM courses WHERE id = $id".update.run.transact(xa).void

