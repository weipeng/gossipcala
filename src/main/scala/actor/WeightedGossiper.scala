package actor

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.SingleMeanGossiper
import message._

class WeightedGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends GossiperActorTrait[Double, SingleMeanGossiper, WeightExtraState] {

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    wState: WeightExtraState): Receive = common(gossiper) orElse {
    case InitMessage(nbs) =>
      val neighbors = nbs + (name -> self)
      context become work(neighbors,
        gossiper.bumpRound(),
        wState.copy(diffuseMatrix = nbs.valuesIterator.map(_ -> 1.0 / nbs.size).toMap))

    case StartMessage(_) =>
      context become work(neighbors, gossip(gossiper, wState), wState)

    case WeightedPushMessage(data, round) =>
      val mailBoxState = wState.mailbox :+ data

      if (mailBoxState.size == neighbors.size) {
        val newState = gossiper.copy(data = mailBoxState.fold(DenseVector(0.0, 0.0))((x, y) => x + y)).compareData()
        if (newState.toStop()) {
          context become work(neighbors, newState, wState.copy(mailbox = mailBoxState))
          self ! StopMessage
        } else {
          context become work(neighbors, newState.bumpRound(), wState.copy(mailbox = Vector.empty))
          self ! StartMessage(None)
        }
      }

    case StopMessage =>
      context become work(neighbors, gossiper.wrap(), wState)
  }

  private def gossip(gossiper: SingleMeanGossiper, wState: WeightExtraState): SingleMeanGossiper = {
    val diffuseMat = wState.diffuseMatrix
    for ((k, v) <- diffuseMat.iterator) {
      k ! WeightedPushMessage(gossiper.data * v, gossiper.roundCount)
    }
    gossiper.copy(messageCount = gossiper.messageCount + diffuseMat.size)
  }

  override val defaultExtraState = WeightExtraState(Map.empty, Vector.empty)
}
