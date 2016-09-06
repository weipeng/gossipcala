package message

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.NodeStatus

import scala.collection.immutable.Map

case class PushMessage(data: Double)
case class PullMessage(data: Double)
case class WeightedPushMessage(data: DenseVector[Double], round: Int)

case class InitMessage(neighbors: Map[String, ActorRef])
case object StartMessage
case object StopMessage

case object CheckState
case class NodeState(status: NodeStatus.Value, round: Int, estimate: Double)

case object KillMessage

case object PushSignal
