package actor

import akka.actor.ActorRef
import akka.event.Logging
import gossiper.SingleMeanGossiper
import message._

import scala.util.Random


class PushPullGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  lazy val rnd = new Random(System.currentTimeMillis)
  lazy val log = Logging(context.system, this)

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper)

    case PushMessage(value) =>
      log.info(s"${self.toString} receives a PUSH message from ${sender.toString}")
      val (msg, state) = makePullMessage(gossiper)
      sender ! msg
      log.info(s"${self.toString} returns a PULL message to ${sender.toString}")
      context become work(neighbors, state.bumpRound.update(value))

    case PullMessage(value) =>
      val newState = gossiper.update(value).compareData()
      log.info(s"${self.toString} receives a PULL message to ${sender.toString}")
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
    log.info(s"${self.toString} sends a PUSH message to ${sender.toString}")
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushMessage, SingleMeanGossiper) =
    (PushMessage(gossiper.data(1)), gossiper.bumpMessage())

  private def makePullMessage(gossiper: SingleMeanGossiper): (PullMessage, SingleMeanGossiper) =
    (PullMessage(gossiper.data(1)), gossiper.bumpMessage())
}
