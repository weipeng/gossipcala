package message

import akka.actor.ActorRef
import breeze.linalg.DenseVector

import scala.collection.immutable.Map

case class PushMessage(data: Double)
case class PullMessage(data: Double)
case class WeightedPushMessage(data: DenseVector[Double], round: Int)

case class InitMessage(neighbors: Map[String, ActorRef])
case object StartMessage
case object StopMessage
case object AskStateAndEstimateMessage
case object KillMessage

case object PushSignal
