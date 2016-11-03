package actor

import akka.actor.{Actor, ActorRef}
import gossiper.AggregateGossiper
import message.{CheckState, KillMessage, NodeState}

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
  * Author: yanyang.wang
  * Date: 07/09/2016
  */
trait GossiperActorTrait[T, A <: AggregateGossiper] extends Actor {
  val name: String
  protected val gossiper: A
  protected def gossip(target: ActorRef, gossiper: A, isResend: Boolean): A
  lazy val rnd = new Random(System.currentTimeMillis)

  def common(g: A): Receive = {
    case KillMessage =>
      context.stop(self)

    case CheckState =>
      sender ! NodeState(name,
        g.status,
        g.roundCount,
        g.wastedRoundCount,
        g.messageCount,
        g.busyMessageCount,
        g.estimate())
  }

  override def receive: Receive = work(false, Map.empty, gossiper)
  def work(busyState: Boolean, neighbors: Map[String, ActorRef], gossiper: A): Receive

  protected def waitTime: FiniteDuration

  protected def sendSelf(msg: Any, requireDelay: Boolean) = {
    if (requireDelay) context.system.scheduler.scheduleOnce(waitTime)(self ! msg)
    else self ! msg
  }
}

object GossiperActorTrait{
  def extractName(actor: ActorRef) = actor.path.toString.split("/").last
}
