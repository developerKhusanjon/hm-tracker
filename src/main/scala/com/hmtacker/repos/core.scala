package com.hmtacker.repos

import cats.effect.IO
import com.hmtacker.domain.{Course, User, *}

import java.util.UUID

trait UserRepository:
  def create(user: User): IO[User]
  def findById(id: UserId): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
  def update(user: User): IO[User]
  def updateFcmToken(userId: UserId, token: String): IO[Unit]

trait CourseRepository:
  def create(course: Course): IO[Course]
  def findById(id: CourseId): IO[Option[Course]]
  def findByUserId(userId: UserId): IO[List[Course]]
  def update(course: Course): IO[Course]
  def delete(id: CourseId): IO[Unit]

trait TaskRepository:
  def create(task: Task): IO[Task]
  def findById(id: TaskId): IO[Option[Task]]
  def findByUserId(userId: UserId): IO[List[Task]]
  def findByUserIdAndState(userId: UserId, state: TaskState): IO[List[Task]]
  def findDueSoon(minutes: Int): IO[List[Task]]
  def findOverdue(): IO[List[Task]]
  def update(task: Task): IO[Task]
  def updateState(taskId: TaskId, newState: TaskState): IO[Option[Task]]
  def delete(id: TaskId): IO[Unit]

trait NotificationRepository:
  def create(notification: Notification): IO[Notification]
  def findPendingNotifications(): IO[List[Notification]]
  def markAsSent(id: UUID): IO[Unit]
  def markAsDelivered(id: UUID): IO[Unit]

trait TaskStateTransitionRepository:
  def create(transition: TaskStateTransition): IO[TaskStateTransition]
  def findByTaskId(taskId: TaskId): IO[List[TaskStateTransition]]