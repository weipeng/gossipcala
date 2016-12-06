package actor

import akka.actor.{Props, ActorLogging, ActorRef}
import gossiper.SingleMeanGossiper
import message._

class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends BinaryGossiperTrait[Double, SingleMeanGossiper, PushPullExtraState] with ActorLogging {

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    ppState: PushPullExtraState): Receive = common(gossiper) orElse {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper, ppState)

    case PushMessage(value) =>
      if (ppState.busyState) {
        val (msg, state) = makePullMessage(gossiper)
        sender ! msg
        val newState = state.bumpRound.update(value)
        log.debug(s"$name receive push $value, reply ${GossiperActorTrait.extractName(sender)} with ${msg.data} and update to ${newState.data(1)}")
        context become work(neighbors, newState, ppState)
      } else {
        log.debug(s"$name is in BusyState when ${GossiperActorTrait.extractName(sender)} request")
        sender ! BusyState
      }

    case PullMessage(value) =>
      val newState = gossiper.update(value).compareData()
      log.debug(s"$name receive pull $value from ${GossiperActorTrait.extractName(sender)} and update to ${newState.data(1)}")
      context become work(neighbors, newState, ppState.copy(busyState = true))
      if (newState.toStop()) {
        self ! StopMessage
      } else {
        self ! StartMessage(None)
      }

    case BusyState =>
      context become work(neighbors, gossiper, ppState.copy(busyState = true))
      val nextTarget = nextNeighbor(neighbors, Some(sender()))
      sendSelfWithDelay(StartMessage(Some(nextTarget)))

    case StopMessage =>
      log.debug(s"$name stopped")
      context become work(neighbors, gossiper.wrap(), ppState)

    case StartMessage(t) =>
      val target = t.getOrElse(nextNeighbor(neighbors, None))
      context become work(neighbors, gossip(target, gossiper, t.isDefined), ppState.copy(busyState = false))

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def gossip(target: ActorRef, gossiper: SingleMeanGossiper, isResend: Boolean): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    log.debug(s"$name push ${GossiperActorTrait.extractName(target)} with ${msg.data}")
    if (isResend) state.bumpBusyMessage()
    else state.bumpRound()
  }

  override val defaultExtraState = PushPullExtraState(false)

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())
}

object PushPullGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new PushPullGossiper(name, gossiper))
}
