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

class PostgresTaskRepository(xa: Transactor[IO]) extends TaskRepository:

  def create(task: Task): IO[Task] =
    sql"""
      INSERT INTO tasks (id, user_id, course_id, title, description, priority, state,
                        due_date, estimated_duration, actual_duration, tags, created_at, updated_at)
      VALUES (${task.id}, ${task.userId}, ${task.courseId}, ${task.title}, ${task.description},
              ${task.priority}, ${task.state}, ${task.dueDate}, ${task.estimatedDuration},
              ${task.actualDuration}, ${task.tags}, ${task.createdAt}, ${task.updatedAt})
    """.update.run.transact(xa) *> IO.pure(task)

  def findById(id: TaskId): IO[Option[Task]] =
    sql"""
      SELECT id, user_id, course_id, title, description, priority, state, due_date,
             estimated_duration, actual_duration, tags, created_at, updated_at, completed_at
      FROM tasks WHERE id = $id
    """.query[Task].option.transact(xa)

  def findByUserId(userId: UserId): IO[List[Task]] =
    sql"""
      SELECT id, user_id, course_id, title, description, priority, state, due_date,
             estimated_duration, actual_duration, tags, created_at, updated_at, completed_at
      FROM tasks WHERE user_id = $userId ORDER BY due_date
    """.query[Task].to[List].transact(xa)

  def findByUserIdAndState(userId: UserId, state: TaskState): IO[List[Task]] =
    sql"""
      SELECT id, user_id, course_id, title, description, priority, state, due_date,
             estimated_duration, actual_duration, tags, created_at, updated_at, completed_at
      FROM tasks WHERE user_id = $userId AND state = $state ORDER BY due_date
    """.query[Task].to[List].transact(xa)

  def findDueSoon(minutes: Int): IO[List[Task]] =
    val threshold = LocalDateTime.now().plusMinutes(minutes)
    sql"""
      SELECT id, user_id, course_id, title, description, priority, state, due_date,
             estimated_duration, actual_duration, tags, created_at, updated_at, completed_at
      FROM tasks 
      WHERE due_date <= $threshold AND state IN ('Created', 'InProgress')
    """.query[Task].to[List].transact(xa)

  def findOverdue(): IO[List[Task]] =
    val now = LocalDateTime.now()
    sql"""
      SELECT id, user_id, course_id, title, description, priority, state, due_date,
             estimated_duration, actual_duration, tags, created_at, updated_at, completed_at
      FROM tasks 
      WHERE due_date < $now AND state IN ('Created', 'InProgress')
    """.query[Task].to[List].transact(xa)

  def update(task: Task): IO[Task] =
    sql"""
      UPDATE tasks 
      SET title = ${task.title}, description = ${task.description}, priority = ${task.priority},
          state = ${task.state}, due_date = ${task.dueDate}, estimated_duration = ${task.estimatedDuration},
          actual_duration = ${task.actualDuration}, tags = ${task.tags}, updated_at = ${task.updatedAt},
          completed_at = ${task.completedAt}
      WHERE id = ${task.id}
    """.update.run.transact(xa) *> IO.pure(task)

  def updateState(taskId: TaskId, newState: TaskState): IO[Option[Task]] =
    val now = LocalDateTime.now()
    val completedAt = if newState == TaskState.Completed then Some(now) else None

    sql"""
      UPDATE tasks 
      SET state = $newState, updated_at = $now, completed_at = $completedAt
      WHERE id = $taskId
    """.update.run.transact(xa) *> findById(taskId)

  def delete(id: TaskId): IO[Unit] =
    sql"DELETE FROM tasks WHERE id = $id".update.run.transact(xa).void