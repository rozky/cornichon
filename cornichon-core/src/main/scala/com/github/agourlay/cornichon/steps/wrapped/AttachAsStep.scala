package com.github.agourlay.cornichon.steps.wrapped

import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.core.Done._
import com.github.agourlay.cornichon.util.Timing._

import monix.execution.Scheduler

// Steps are wrapped/indented with a specific title
case class AttachAsStep(title: String, nested: List[Step]) extends WrapperStep {

  override def setTitle(newTitle: String) = copy(title = newTitle)

  override def run(engine: Engine)(initialRunState: RunState)(implicit scheduler: Scheduler) =
    withDuration {
      val nestedRunState = initialRunState.forNestedSteps(nested)
      engine.runSteps(nestedRunState)
    }.map {
      case (run, executionTime) ⇒

        val (attachedRunState, res) = run

        val nestedLogs = attachedRunState.logs
        val initialDepth = initialRunState.depth
        val (fullLogs, xor) = res.fold(
          failedStep ⇒ {
            val failureLogs = failedTitleLog(initialDepth) +: nestedLogs :+ FailureLogInstruction(s"$title - Failed", initialDepth)
            (failureLogs, Left(failedStep))
          },
          _ ⇒ {
            val successLogs = successTitleLog(initialDepth) +: nestedLogs :+ SuccessLogInstruction(s"$title - Succeeded", initialDepth, Some(executionTime))
            (successLogs, rightDone)
          }
        )
        (initialRunState.withSession(attachedRunState.session).appendLogs(fullLogs), xor)
    }
}
