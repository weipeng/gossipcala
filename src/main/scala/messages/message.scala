package message

import akka.actor.{Actor, ActorRef}
import scala.collection.immutable.Map
import breeze.linalg.DenseVector


case class PushMessage(val data: Double) 
case class PullMessage(val data: Double)
case class WeightedPushMessage(val data: DenseVector[Double], val round: Int)

case class InitMessage(val neighbors: Map[String, ActorRef])
case object StartMessage
case object StopMessage
case object AskStateAndEstimateMessage
case object KillMessage

case object PushSignal
