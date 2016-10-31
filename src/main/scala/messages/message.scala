package message

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.GossiperStatus

import scala.collection.immutable.Map

case class PushMessage(data: Double)
case class PullMessage(data: Double)
case object InvalidState
case class WeightedPushMessage(data: DenseVector[Double], round: Int)
case class PushSumMessage(data: DenseVector[Double])

case object PushSignal
case object UpdateSignal

case class InitMessage(neighbors: Map[String, ActorRef])
case class StartMessage(target: Option[ActorRef])
case object StopMessage

case object CheckState
case class NodeState(nodeName: String,
                     status: GossiperStatus.Value,
                     roundCount: Int,
                     wastedRoundCount: Int,
                     messageCount: Int,
                     invalidMessageCount: Int,
                     estimate: Double)

case object KillMessage
