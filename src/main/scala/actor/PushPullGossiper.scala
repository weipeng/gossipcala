package actor

import akka.actor.ActorRef
import gossiper.SingleMeanGossiper
import message._

import scala.util.Random


class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  val rnd = new Random

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper.bumpRound())

    case PushMessage(value) =>
      sender ! makePullMessage()
      context become work(neighbors, gossiper.update(value))

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
      sender ! NodeState(gossiper.status, gossiper.roundCount, gossiper.estimate())

    case msg =>
      println(s"Unexpected message $msg received")
  }

  override def gossip(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val nbs = neighbors.values.toArray
    val neighbor = nbs(rnd.nextInt(neighbors.size))
    neighbor ! makePushMessage()
    gossiper.bumpRound()
  }

  private def makePushMessage() = PushMessage(gossiper.data(1))

  private def makePullMessage() = PullMessage(gossiper.data(1))

}
