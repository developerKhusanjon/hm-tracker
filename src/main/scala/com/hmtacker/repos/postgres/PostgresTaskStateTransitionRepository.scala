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

class PostgresTaskStateTransitionRepository(xa: Transactor[IO]) extends TaskStateTransitionRepository:

  def create(transition: TaskStateTransition): IO[TaskStateTransition] =
    sql"""
      INSERT INTO task_state_transitions (task_id, from_state, to_state, timestamp, reason)
      VALUES (${transition.taskId}, ${transition.fromState}, ${transition.toState}, 
              ${transition.timestamp}, ${transition.reason})
    """.update.run.transact(xa) *> IO.pure(transition)

  def findByTaskId(taskId: TaskId): IO[List[TaskStateTransition]] =
    sql"""
      SELECT task_id, from_state, to_state, timestamp, reason
      FROM task_state_transitions 
      WHERE task_id = $taskId
      ORDER BY timestamp DESC
    """.query[TaskStateTransition].to[List].transact(xa)
