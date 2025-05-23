package com.hmtacker.domain

import java.time.LocalDateTime
import java.util.UUID
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

// Task State Machine
enum TaskState derives Encoder.AsObject, Decoder:
  case Created
  case InProgress
  case Completed
  case Overdue
  case Archived

object TaskState:
  def validTransitions(from: TaskState): Set[TaskState] = from match
    case Created => Set(InProgress, Completed, Overdue, Archived)
    case InProgress => Set(Completed, Overdue, Archived)
    case Completed => Set(Archived)
    case Overdue => Set(Completed, Archived)
    case Archived => Set.empty

  def canTransition(from: TaskState, to: TaskState): Boolean =
    validTransitions(from).contains(to)

// Priority levels for tasks
enum Priority derives Encoder.AsObject, Decoder:
  case Low, Medium, High, Critical

// Subject categories
enum Subject derives Encoder.AsObject, Decoder:
  case Mathematics, Science, English, History, ComputerScience, Art, Music, Other

// Core domain models
case class UserId(value: UUID)

object UserId:
  given Encoder[UserId] = Encoder.encodeUUID.contramap(_.value)

  given Decoder[UserId] = Decoder.decodeUUID.map(UserId.apply)

case class TaskId(value: UUID)

object TaskId:
  given Encoder[TaskId] = Encoder.encodeUUID.contramap(_.value)

  given Decoder[TaskId] = Decoder.decodeUUID.map(TaskId.apply)
  
case class CourseId(value: UUID)

object CourseId:
  given Encoder[CourseId] = Encoder.encodeUUID.contramap(_.value)

  given Decoder[CourseId] = Decoder.decodeUUID.map(CourseId.apply)

case class User(
                 id: UserId,
                 email: String,
                 firstName: String,
                 lastName: String,
                 fcmToken: Option[String],
                 createdAt: LocalDateTime,
                 updatedAt: LocalDateTime
               ) derives Encoder.AsObject, Decoder

case class Course(
                   id: CourseId,
                   userId: UserId,
                   name: String,
                   subject: Subject,
                   instructor: String,
                   description: Option[String],
                   createdAt: LocalDateTime
                 ) derives Encoder.AsObject, Decoder

case class Task(
                 id: TaskId,
                 userId: UserId,
                 courseId: Option[CourseId],
                 title: String,
                 description: Option[String],
                 priority: Priority,
                 state: TaskState,
                 dueDate: LocalDateTime,
                 estimatedDuration: Option[Int], // in minutes
                 actualDuration: Option[Int], // in minutes
                 tags: List[String],
                 createdAt: LocalDateTime,
                 updatedAt: LocalDateTime,
                 completedAt: Option[LocalDateTime]
               ) derives Encoder.AsObject, Decoder

case class TaskStateTransition(
                                taskId: TaskId,
                                fromState: TaskState,
                                toState: TaskState,
                                timestamp: LocalDateTime,
                                reason: Option[String]
                              ) derives Encoder.AsObject, Decoder

case class Notification(
                         id: UUID,
                         userId: UserId,
                         taskId: TaskId,
                         title: String,
                         body: String,
                         scheduledAt: LocalDateTime,
                         sentAt: Option[LocalDateTime],
                         delivered: Boolean
                       ) derives Encoder.AsObject, Decoder