package actor

import akka.actor.{ActorLogging, ActorRef}
import gossiper.SingleMeanGossiper
import message._

import scala.util.Random


class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] with ActorLogging{

  lazy val rnd = new Random(System.currentTimeMillis)

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper)

    case PushMessage(value) =>
      val (msg, state) = makePullMessage(gossiper)
      sender ! msg
      val newState = state.bumpRound.update(value)
      log.debug(s"$name receive push $value, reply ${GossiperActorTrait.extractName(sender)} with ${msg.data} and update to ${newState.data(1)}")
      context become work(neighbors, newState)

    case PullMessage(value) =>
      val newState = gossiper.update(value).compareData()
      log.debug(s"$name receive pull $value from ${GossiperActorTrait.extractName(sender)} and update to ${newState.data(1)}")
      if (newState.toStop()) {
        self ! StopMessage
        context become work(neighbors, newState)
      } else {
        context become work(neighbors, gossip(neighbors, newState))
      }

    case StopMessage =>
      context become work(neighbors, gossiper.wrap())

    case StartMessage =>
      context become work(neighbors, gossip(neighbors, gossiper))

    case KillMessage =>
      context.stop(self)

    case CheckState =>
      sender ! NodeState(name,
        gossiper.status,
        gossiper.roundCount,
        gossiper.wastedRoundCount,
        gossiper.messageCount,
        gossiper.estimate())

    case msg =>
      println(s"Unexpected message $msg received")
  }

  override def gossip(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val nbs = neighbors.values.toArray
    val neighbor = nbs(rnd.nextInt(neighbors.size))
    val (msg, state) = makePushMessage(gossiper)
    neighbor ! msg
    log.debug(s"$name push ${GossiperActorTrait.extractName(neighbor)} with ${msg.data}")
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())
}
