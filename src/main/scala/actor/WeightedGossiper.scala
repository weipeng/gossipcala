package actor

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.SingleMeanGossiper
import message._

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps


class WeightedGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  var diffuseMat: Map[ActorRef, Double] = Map()
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()
  var nextRndMailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(nbs) =>
      val neighbors = nbs + (name -> self)
      setDiffuseMatrix(neighbors)
      context become work(neighbors, gossiper.bumpRound())

    case StartMessage =>
      context become work(neighbors, gossip(neighbors, gossiper))

    case PushSignal =>
      context become work(neighbors, gossip(neighbors, gossiper))

    case WeightedPushMessage(_data, _rounds) =>
      /*if (_rounds == rounds+1) {
          nextRndMailbox.append(_data)
      } else if (_rounds == rounds) {
          mailbox.append(_data)
      }*/
      mailbox.append(_data)

      if (mailbox.size == neighbors.size) {
        val newState = gossiper.copy(data = mailbox.fold(DenseVector(0.0, 0.0))((x, y) => x + y)).compareData()
        if (newState.toStop()) {
          context become work(neighbors, newState)
          self ! StopMessage
        } else {
          mailbox = nextRndMailbox
          nextRndMailbox = new ListBuffer()
          context become work(neighbors, newState.bumpRound())
          self ! PushSignal
        }
      }

    case StopMessage =>
      context become work(neighbors, gossip(neighbors, gossiper.wrap()))

    case KillMessage =>
      context.stop(self)

    case CheckState =>
      sender ! NodeState(gossiper.status, gossiper.roundCount, 
                         gossiper.wastedRoundCount, gossiper.messageCount,
                         gossiper.estimate())

  }

  override def gossip(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    for ((k, v) <- diffuseMat.iterator) {
      k ! WeightedPushMessage(gossiper.data * v, gossiper.roundCount)
    }
    gossiper.copy(messageCount = gossiper.messageCount + diffuseMat.size)
  }

  private def setDiffuseMatrix(nbs: Map[String, ActorRef]) {
    val numNeighbors = nbs.size
    diffuseMat = nbs.valuesIterator.map(_ -> 1.0 / numNeighbors) toMap
  }
}
