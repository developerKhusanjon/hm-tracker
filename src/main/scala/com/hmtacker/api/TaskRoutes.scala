package com.hmtacker.api

import cats.effect.IO
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import java.time.LocalDateTime
import java.util.UUID
import com.hmtacker.domain.*
import com.hmtacker.services.*

// Request/Response DTOs
case class CreateTaskRequest(
                              courseId: Option[String],
                              title: String,
                              description: Option[String],
                              priority: String,
                              dueDate: String, // ISO format
                              estimatedDuration: Option[Int],
                              tags: List[String]
                            )

case class UpdateTaskStateRequest(
                                   state: String,
                                   reason: Option[String]
                                 )

case class TaskResponse(
                         id: String,
                         courseId: Option[String],
                         title: String,
                         description: Option[String],
                         priority: String,
                         state: String,
                         dueDate: String,
                         estimatedDuration: Option[Int],
                         actualDuration: Option[Int],
                         tags: List[String],
                         createdAt: String,
                         updatedAt: String,
                         completedAt: Option[String]
                       )

object TaskResponse:
  def fromDomain(task: Task): TaskResponse =
    TaskResponse(
      id = task.id.value.toString,
      courseId = task.courseId.map(_.value.toString),
      title = task.title,
      description = task.description,
      priority = task.priority.toString,
      state = task.state.toString,
      dueDate = task.dueDate.toString,
      estimatedDuration = task.estimatedDuration,
      actualDuration = task.actualDuration,
      tags = task.tags,
      createdAt = task.createdAt.toString,
      updatedAt = task.updatedAt.toString,
      completedAt = task.completedAt.map(_.toString)
    )

class TaskRoutes(taskService: TaskService):

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "users" / UUIDVar(userId) / "tasks" =>
      taskService.taskRepo.findByUserId(UserId(userId))
        .map(_.map(TaskResponse.fromDomain))
        .flatMap(tasks => Ok(tasks.asJson))

    case GET -> Root / "users" / UUIDVar(userId) / "tasks" / "state" / state =>
      Either.catchNonFatal(TaskState.valueOf(state)) match
        case Right(taskState) =>
          taskService.taskRepo.findByUserIdAndState(UserId(userId), taskState)
            .map(_.map(TaskResponse.fromDomain))
            .flatMap(tasks => Ok(tasks.asJson))
        case Left(_) =>
          BadRequest(s"Invalid state: $state")

    case req @ POST -> Root / "users" / UUIDVar(userId) / "tasks" =>
      for
        createReq <- req.as[CreateTaskRequest]
        courseId = createReq.courseId.map(id => CourseId(UUID.fromString(id)))
        priority <- IO.fromEither(
                                  Either
                                        .catchOnly[IllegalArgumentException](Priority.valueOf(createReq.priority))
                                        .leftMap(_ => new IllegalArgumentException("Invalid priority"))
                                      )
        dueDate <- IO.fromEither(
          Either.catchNonFatal(LocalDateTime.parse(createReq.dueDate))
            .left.map(_ => new IllegalArgumentException("Invalid date format"))
        )
        task <- taskService.createTask(
          userId = UserId(userId),
          courseId = courseId,
          title = createReq.title,
          description = createReq.description,
          priority = priority,
          dueDate = dueDate,
          estimatedDuration = createReq.estimatedDuration,
          tags = createReq.tags
        )
        response <- Ok(TaskResponse.fromDomain(task).asJson)
      yield response

    case req @ PUT -> Root / "tasks" / UUIDVar(taskId) / "state" =>
      for
        updateReq <- req.as[UpdateTaskStateRequest]
        newState <- IO.fromEither(
                                  Either
                                        .catchOnly[IllegalArgumentException](TaskState.valueOf(updateReq.state))
                                        .leftMap(_ => new IllegalArgumentException("Invalid state"))
                                    )
        taskOpt <- taskService.updateTaskState(TaskId(taskId), newState, updateReq.reason)
        response <- taskOpt match
          case Some(task) => Ok(TaskResponse.fromDomain(task).asJson)
          case None => NotFound("Task not found")
      yield response

    case PUT -> Root / "tasks" / UUIDVar(taskId) / "start" =>
      taskService.startTask(TaskId(taskId)).flatMap {
        case Some(task) => Ok(TaskResponse.fromDomain(task).asJson)
        case None => NotFound("Task not found")
      }

    case PUT -> Root / "tasks" / UUIDVar(taskId) / "complete" =>
      taskService.completeTask(TaskId(taskId)).flatMap {
        case Some(task) => Ok(TaskResponse.fromDomain(task).asJson)
        case None => NotFound("Task not found")
      }

    case GET -> Root / "tasks" / UUIDVar(taskId) =>
      taskService.taskRepo.findById(TaskId(taskId)).flatMap {
        case Some(task) => Ok(TaskResponse.fromDomain(task).asJson)
        case None => NotFound("Task not found")
      }
  }