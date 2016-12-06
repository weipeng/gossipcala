package message

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.GossiperStatus

import scala.collection.immutable.Map

sealed trait Message

case class PushMessage(data: Double) extends Message
case class PullMessage(data: Double) extends Message
case object BusyState extends Message
case class WeightedPushMessage(data: DenseVector[Double], round: Int) extends Message
case class PushSumMessage(data: DenseVector[Double]) extends Message

case object PushSignal extends Message
case object UpdateSignal extends Message

case class InitMessage(neighbors: Map[String, ActorRef]) extends Message
case class StartMessage(target: Option[ActorRef]) extends Message
case object StopMessage extends Message

case object CheckState extends Message
case class NodeState(nodeName: String,
                     status: GossiperStatus.Value,
                     roundCount: Int,
                     wastedRoundCount: Int,
                     messageCount: Int,
                     busyMessageCount: Int,
                     estimate: Double) extends Message

case object KillMessage extends Message
