package actor

import akka.actor.{ActorLogging, ActorRef}
import gossiper.SingleMeanGossiper
import message._

import scala.concurrent.duration._
import scala.language.postfixOps

class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends GossiperActorTrait[Double, SingleMeanGossiper] with ActorLogging {

  override def work(inValidState: Boolean, neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = common(gossiper) orElse {
    case InitMessage(neighbors) =>
      context become work(inValidState, neighbors, gossiper)

    case PushMessage(value) =>
      if (inValidState) {
        val (msg, state) = makePullMessage(gossiper)
        sender ! msg
        val newState = state.bumpRound.update(value)
        log.debug(s"$name receive push $value, reply ${GossiperActorTrait.extractName(sender)} with ${msg.data} and update to ${newState.data(1)}")
        context become work(inValidState, neighbors, newState)
      } else {
        log.debug(s"$name is in InvalidState when ${GossiperActorTrait.extractName(sender)} request")
        sender ! InvalidState
      }

    case PullMessage(value) =>
      val newState = gossiper.update(value).compareData()
      log.debug(s"$name receive pull $value from ${GossiperActorTrait.extractName(sender)} and update to ${newState.data(1)}")
      context become work(true, neighbors, newState)
      if (newState.toStop()) {
        self ! StopMessage
      } else {
        sendSelf(StartMessage(None), false)
      }

    case InvalidState =>
      context become work(true, neighbors, gossiper)
      val nextTarget = nextNeighbour(neighbors, Some(sender()))
      sendSelf(StartMessage(Some(nextTarget)), true)

    case StopMessage =>
      log.debug(s"$name stopped")
      context become work(inValidState, neighbors, gossiper.wrap())

    case StartMessage(t) =>
      val target = t.getOrElse(nextNeighbour(neighbors, None))
      context become work(false, neighbors, gossip(target, gossiper, t.isDefined))

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def nextNeighbour(neighbors: Map[String, ActorRef], banNeighbor: Option[ActorRef]): ActorRef = {
    val eligibleNeighbours = (neighbors.values.toSet -- banNeighbor).toArray[ActorRef]
    (banNeighbor, eligibleNeighbours) match {
      case (Some(n), Array()) => n
      case _ => eligibleNeighbours(rnd.nextInt(eligibleNeighbours.length))
    }
  }

  override def gossip(target: ActorRef, gossiper: SingleMeanGossiper, isResend: Boolean): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    log.debug(s"$name push ${GossiperActorTrait.extractName(target)} with ${msg.data}")
    if (isResend) state.bumpInvalidMessage()
    else state.bumpRound()
  }

  override def waitTime = (rnd.nextInt(10) * 10) millis

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())
}
