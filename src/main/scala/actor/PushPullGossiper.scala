package actor

import akka.actor.ActorRef
import akka.event.LoggingReceive
import gossiper.SingleMeanGossiper
import message._

import scala.util.Random


class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  val rnd = new Random

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper)

    case PushMessage(value) =>
      val (msg, state) = makePullMessage(gossiper)
      sender ! msg
      context become work(neighbors, state.bumpRound.update(value))

    case PullMessage(value) =>
      val newState = gossiper.update(value).compareData()
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
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())
}
