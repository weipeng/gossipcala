package actor

import akka.actor.{Props, ActorLogging, ActorRef}
import gossiper.SingleMeanGossiper
import message._
import breeze.numerics.abs
import com.typesafe.scalalogging.LazyLogging

class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends BinaryGossiperTrait[Double, SingleMeanGossiper, PushPullExtraState] with ActorLogging {

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    ppState: PushPullExtraState): Receive = common(gossiper) orElse {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper, ppState)

    case PushMessage(value) =>
      log.debug(s"${this.name} receive push $value from ${GossiperActorTrait.extractName(sender)} and update to ${gossiper.data(1)} ${gossiper.status}")
      if (ppState.busyState) {
        val (msg, state) = makePullMessage(gossiper)
        sender ! msg
        val newState = update(state.bumpRound(), value)
        context become work(neighbors, newState, ppState)
      } else {
        log.debug(s"$name is in BusyState when ${GossiperActorTrait.extractName(sender)} request")
        sender ! BusyState
      }

    case PullMessage(value) =>
      val newState = update(gossiper, value).compareData()
      context become work(neighbors, newState, ppState.copy(busyState = true))
      if (newState.toStop) self ! StopMessage else self ! StartMessage(None)

      log.debug(s"$name receive pull $value from ${GossiperActorTrait.extractName(sender)} and update to ${newState.data(1)} ${newState.status} ${newState.convergenceCount}")

    case BusyState =>
      context become work(neighbors, gossiper, ppState.copy(busyState = true))
      val nextTarget = nextNeighbor(neighbors, Some(sender()))
      sendSelfWithDelay(StartMessage(Some(nextTarget)))

    case StopMessage =>
      val g = gossiper.wrap()
      context become work(neighbors, g, ppState)
      log.debug(s"$name stopped, status: ${g.status}")

    case StartMessage(t) =>
      val target = t.getOrElse(nextNeighbor(neighbors, None))
      context become work(neighbors, gossip(target, gossiper, t.isDefined), ppState.copy(busyState = false))

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def gossip(target: ActorRef, gossiper: SingleMeanGossiper, isResend: Boolean): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    log.debug(s"[P] $name push to ${GossiperActorTrait.extractName(target)} with ${msg.data}")
    if (isResend) state.bumpBusyMessage()
    else state.bumpRound()
  }

  override val defaultExtraState = PushPullExtraState(false)

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def update(gossiper: SingleMeanGossiper, value: Double): SingleMeanGossiper = {
    val data = gossiper.data
    val s = (data(1) + value) * 0.5
    val wasteQuantity = if (gossiper.isWasted(s)) 1 else 0
    
    data(1) = s
    log.debug(s"${gossiper.name} counter:, ${gossiper.convergenceCount}, ${gossiper.lastMetric} ? ${data(1)}")
    gossiper.copy(data = data,
                  wastedRoundCount = gossiper.wastedRoundCount + wasteQuantity)
  }
}

object PushPullGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new PushPullGossiper(name, gossiper))
}
