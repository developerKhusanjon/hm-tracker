package com.hmtacker.services

import cats.effect.IO
import cats.implicits.*
import java.time.LocalDateTime
import java.util.UUID
import com.hmtacker.domain.*
import com.hmtacker.repos.*

class TaskService(
                   val taskRepo: TaskRepository,
                   transitionRepo: TaskStateTransitionRepository,
                   notificationService: NotificationService
                 ):

  def createTask(
                  userId: UserId,
                  courseId: Option[CourseId],
                  title: String,
                  description: Option[String],
                  priority: Priority,
                  dueDate: LocalDateTime,
                  estimatedDuration: Option[Int],
                  tags: List[String]
                ): IO[Task] =
    val now = LocalDateTime.now()
    val task = Task(
      id = TaskId(UUID.randomUUID()),
      userId = userId,
      courseId = courseId,
      title = title,
      description = description,
      priority = priority,
      state = TaskState.Created,
      dueDate = dueDate,
      estimatedDuration = estimatedDuration,
      actualDuration = None,
      tags = tags,
      createdAt = now,
      updatedAt = now,
      completedAt = None
    )

    for
      createdTask <- taskRepo.create(task)
      _ <- recordStateTransition(task.id, TaskState.Created, TaskState.Created, "Task created")
      _ <- scheduleNotifications(createdTask)
    yield createdTask

  def updateTaskState(taskId: TaskId, newState: TaskState, reason: Option[String] = None): IO[Option[Task]] =
    for
      taskOpt <- taskRepo.findById(taskId)
      result <- taskOpt match
        case Some(task) if TaskState.canTransition(task.state, newState) =>
          for
            updatedTask <- taskRepo.updateState(taskId, newState)
            _ <- recordStateTransition(taskId, task.state, newState, reason.getOrElse("State transition"))
            _ <- updatedTask match
              case Some(t) if newState == TaskState.Completed =>
                notificationService.cancelNotifications(taskId)
              case _ => IO.unit
          yield updatedTask
        case Some(_) =>
          IO.raiseError(new IllegalStateException(s"Invalid state transition to $newState"))
        case None =>
          IO.pure(None)
    yield result

  def startTask(taskId: TaskId): IO[Option[Task]] =
    updateTaskState(taskId, TaskState.InProgress, Some("Task started"))

  def completeTask(taskId: TaskId, actualDuration: Option[Int] = None): IO[Option[Task]] =
    for
      taskOpt <- taskRepo.findById(taskId)
      result <- taskOpt match
        case Some(task) =>
          val updatedTask = task.copy(
            actualDuration = actualDuration.orElse(task.actualDuration),
            updatedAt = LocalDateTime.now()
          )
          taskRepo.update(updatedTask) *>
            updateTaskState(taskId, TaskState.Completed, Some("Task completed"))
        case None => IO.pure(None)
    yield result

  def markOverdue(): IO[List[Task]] =
    for
      overdueTasks <- taskRepo.findOverdue()
      updatedTasks <- overdueTasks.traverse(task =>
        updateTaskState(task.id, TaskState.Overdue, Some("Task overdue")).map(_.get)
      )
    yield updatedTasks

  private def recordStateTransition(
                                     taskId: TaskId,
                                     fromState: TaskState,
                                     toState: TaskState,
                                     reason: String
                                   ): IO[TaskStateTransition] =
    val transition = TaskStateTransition(
      taskId = taskId,
      fromState = fromState,
      toState = toState,
      timestamp = LocalDateTime.now(),
      reason = Some(reason)
    )
    transitionRepo.create(transition)

  private def scheduleNotifications(task: Task): IO[Unit] =
    notificationService.scheduleTaskNotifications(task)

class NotificationService(
                           notificationRepo: NotificationRepository,
                           fcmService: FcmService,
                           userRepo: UserRepository
                         ):

  def scheduleTaskNotifications(task: Task): IO[Unit] =
    val notifications = generateNotificationSchedule(task)
    notifications.traverse_(notificationRepo.create)

  def sendPendingNotifications(): IO[Unit] =
    for
      pending <- notificationRepo.findPendingNotifications()
      now = LocalDateTime.now()
      ready = pending.filter(notification => notification.scheduledAt.isBefore(now) || notification.scheduledAt.isEqual(now))
      _ <- ready.traverse_(sendNotification)
    yield ()

  def cancelNotifications(taskId: TaskId): IO[Unit] =
    // In a real implementation, you'd mark notifications as cancelled
    IO.unit

  private def sendNotification(notification: Notification): IO[Unit] =
    for
      userOpt <- userRepo.findById(notification.userId)
      _ <- userOpt match
        case Some(user) if user.fcmToken.isDefined =>
          fcmService.sendNotification(
            user.fcmToken.get,
            notification.title,
            notification.body
          ) *> notificationRepo.markAsSent(notification.id)
        case _ => IO.unit
    yield ()

  private def generateNotificationSchedule(task: Task): List[Notification] =
    val baseId = UUID.randomUUID()
    List(
      // 24 hours before
      Notification(
        id = UUID.randomUUID(),
        userId = task.userId,
        taskId = task.id,
        title = "Assignment Due Tomorrow",
        body = s"${task.title} is due tomorrow at ${task.dueDate.toLocalTime}",
        scheduledAt = task.dueDate.minusHours(24),
        sentAt = None,
        delivered = false
      ),
      // 2 hours before  
      Notification(
        id = UUID.randomUUID(),
        userId = task.userId,
        taskId = task.id,
        title = "Assignment Due Soon",
        body = s"${task.title} is due in 2 hours",
        scheduledAt = task.dueDate.minusHours(2),
        sentAt = None,
        delivered = false
      ),
      // 30 minutes before
      Notification(
        id = UUID.randomUUID(),
        userId = task.userId,
        taskId = task.id,
        title = "Assignment Due Very Soon",
        body = s"${task.title} is due in 30 minutes!",
        scheduledAt = task.dueDate.minusMinutes(30),
        sentAt = None,
        delivered = false
      )
    ).filter(_.scheduledAt.isAfter(LocalDateTime.now()))