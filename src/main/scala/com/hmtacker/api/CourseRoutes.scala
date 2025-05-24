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
import com.hmtacker.repos.*

class CourseRoutes(courseRepo: CourseRepository):

  case class CreateCourseRequest(
                                  name: String,
                                  subject: String,
                                  instructor: String,
                                  description: Option[String]
                                )

  case class CourseResponse(
                             id: String,
                             name: String,
                             subject: String,
                             instructor: String,
                             description: Option[String],
                             createdAt: String
                           )

  object CourseResponse:
    def fromDomain(course: Course): CourseResponse =
      CourseResponse(
        id = course.id.value.toString,
        name = course.name,
        subject = course.subject.toString,
        instructor = course.instructor,
        description = course.description,
        createdAt = course.createdAt.toString
      )

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / "users" / UUIDVar(userId) / "courses" =>
      for
        createReq <- req.as[CreateCourseRequest]
        subject <- IO.fromEither(
                        Either
                          .catchOnly[IllegalArgumentException](Subject.valueOf(createReq.subject))
                          .leftMap(_ => new IllegalArgumentException("Invalid subject"))
                        )
        course = Course(
          id = CourseId(UUID.randomUUID()),
          userId = UserId(userId),
          name = createReq.name,
          subject = subject,
          instructor = createReq.instructor,
          description = createReq.description,
          createdAt = LocalDateTime.now()
        )
        createdCourse <- courseRepo.create(course)
        response <- Ok(CourseResponse.fromDomain(createdCourse).asJson)
      yield response

    case GET -> Root / "users" / UUIDVar(userId) / "courses" =>
      courseRepo.findByUserId(UserId(userId))
        .map(_.map(CourseResponse.fromDomain))
        .flatMap(courses => Ok(courses.asJson))

    case GET -> Root / "courses" / UUIDVar(courseId) =>
      courseRepo.findById(CourseId(courseId)).flatMap {
        case Some(course) => Ok(CourseResponse.fromDomain(course).asJson)
        case None => NotFound("Course not found")
      }

    case DELETE -> Root / "courses" / UUIDVar(courseId) =>
      courseRepo.delete(CourseId(courseId)) *> Ok("Course deleted successfully")
  }