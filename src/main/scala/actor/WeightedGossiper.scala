package actor

import akka.actor.{ActorLogging, ActorRef, Props}
import breeze.linalg.DenseVector
import gossiper.SingleMeanGossiper
import message._

class WeightedGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends GossiperActorTrait[Double, SingleMeanGossiper, WeightExtraState] with ActorLogging {

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    wState: WeightExtraState): Receive = common(gossiper) orElse {
    case InitMessage(nbs) =>
      val neighbors = nbs + (name -> self)
      context become work(neighbors,
        gossiper.bumpRound(),
        wState.copy(diffuseMatrix = neighbors.values.map(_ -> 1.0 / neighbors.size).toMap))

    case StartMessage(_) =>
      context become work(neighbors, gossip(gossiper, wState), wState)

    case WeightedPushMessage(data, gossiper.roundCount) =>
      log.debug(s"$name in ${gossiper.roundCount} receive $data from ${GossiperActorTrait.extractName(sender)}")
      val mailBoxState = wState.mailbox :+ data
      if (mailBoxState.size == neighbors.size) {
        val newState = gossiper.copy(data = mailBoxState.fold(DenseVector(0.0, 0.0))((x, y) => x + y)).compareData()
        if (newState.toStop()) {
          context become work(neighbors, newState, wState.copy(mailbox = Vector.empty))
          self ! StopMessage
        } else {
          context become work(neighbors, newState.bumpRound(), wState.copy(mailbox = Vector.empty))
          self ! StartMessage(None)
        }
      } else {
        context become work(neighbors, gossiper, wState.copy(mailbox = mailBoxState))
      }

    case StopMessage =>
      context become work(neighbors, gossiper.wrap(), wState)
  }

  private def gossip(gossiper: SingleMeanGossiper, wState: WeightExtraState): SingleMeanGossiper = {
    val diffuseMat = wState.diffuseMatrix
    diffuseMat.foreach { case (k, v) =>
      k ! WeightedPushMessage(gossiper.data * v, gossiper.roundCount)
    }
    gossiper.copy(messageCount = gossiper.messageCount + diffuseMat.size)
  }

  override val defaultExtraState = WeightExtraState(Map.empty, Vector.empty)
}

object WeightedGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new WeightedGossiper(name, gossiper))
}
