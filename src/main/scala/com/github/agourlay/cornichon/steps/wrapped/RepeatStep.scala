package com.github.agourlay.cornichon.steps.wrapped

import com.github.agourlay.cornichon.core._

import scala.concurrent.ExecutionContext

case class RepeatStep(nested: Vector[Step], occurence: Int) extends WrapperStep {

  require(occurence > 0, "repeat block must contain a positive number of occurence")

  val title = s"Repeat block with occurence '$occurence'"

  def run(engine: Engine, session: Session, depth: Int)(implicit ec: ExecutionContext) = {
    val titleLog = DefaultLogInstruction(title, depth)
    val (repeatRes, executionTime) = engine.withDuration {
      // Session not propagated through repeat calls
      Vector.range(0, occurence).map(i ⇒ engine.runSteps(nested, session, Vector.empty, depth + 1))
    }
    if (repeatRes.forall(_.isSuccess == true)) {
      val fullLogs = titleLog +: repeatRes.flatMap(_.logs) :+ SuccessLogInstruction(s"Repeat block with occurence $occurence succeeded", depth, Some(executionTime))
      SuccessRunSteps(repeatRes.last.session, fullLogs)
    } else {
      val fullLogs = titleLog +: repeatRes.flatMap(_.logs) :+ FailureLogInstruction(s"Repeat block with occurence $occurence failed", depth, Some(executionTime))
      FailedRunSteps(nested.last, RepeatBlockContainFailedSteps, fullLogs, repeatRes.last.session)
    }
  }
}