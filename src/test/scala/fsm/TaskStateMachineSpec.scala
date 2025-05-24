package fsm

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import java.time.LocalDateTime
import java.util.UUID
import com.hmtacker.domain.*

class TaskStateMachineSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  "TaskState transitions" - {

    "should allow valid transitions from Created state" in {
      TaskState.canTransition(TaskState.Created, TaskState.InProgress) shouldBe true
      TaskState.canTransition(TaskState.Created, TaskState.Completed) shouldBe true
      TaskState.canTransition(TaskState.Created, TaskState.Overdue) shouldBe true
      TaskState.canTransition(TaskState.Created, TaskState.Archived) shouldBe true
    }

    "should not allow invalid transitions" in {
      TaskState.canTransition(TaskState.Completed, TaskState.InProgress) shouldBe false
      TaskState.canTransition(TaskState.Archived, TaskState.Created) shouldBe false
    }

    "should allow completion from InProgress and Overdue" in {
      TaskState.canTransition(TaskState.InProgress, TaskState.Completed) shouldBe true
      TaskState.canTransition(TaskState.Overdue, TaskState.Completed) shouldBe true
    }
  }