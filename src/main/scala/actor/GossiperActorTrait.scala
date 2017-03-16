package actor

import akka.actor.{Actor, ActorRef}
import breeze.linalg.DenseVector
import gossiper.AggregateGossiper
import message.{Message, CheckState, KillMessage, NodeState}

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/**
  * Author: yanyang.wang
  * Date: 07/09/2016
  */
trait GossiperActorTrait[T, A <: AggregateGossiper, E <: ExtraState] extends Actor {
  val name: String
  protected val gossiper: A
  lazy val rnd = new Random(System.currentTimeMillis)

  def common(g: A): Receive = {
    case KillMessage =>
      context.stop(self)

    case check: CheckState =>
      sender ! NodeState(
        name,
        check.checkRound,
        g.status,
        g.roundCount,
        g.wastedRoundCount,
        g.messageCount,
        g.busyMessageCount,
        g.estimate())
  }

  val defaultExtraState: E

  override def receive: Receive = work(Map.empty, gossiper, defaultExtraState)
  def work(neighbors: Map[String, ActorRef], gossiper: A, extraState: E): Receive

  protected def waitTime: FiniteDuration = (rnd.nextInt(10) * 10) millis

  protected def sendSelfWithDelay(msg: Message): Unit = context.system.scheduler.scheduleOnce(waitTime)(self ! msg)

}

object GossiperActorTrait{
  def extractName(actor: ActorRef) = actor.path.toString.split("/").last
}

trait ExtraState

final case object EmptyState extends ExtraState

final case class PushPullExtraState(busyState: Boolean) extends ExtraState

final case class WeightExtraState(diffuseMatrix: Map[ActorRef, Double], mailbox: Vector[DenseVector[Double]]) extends ExtraState

final case class PushSumExtraState(mailBox: Vector[DenseVector[Double]]) extends ExtraState
