package actor

import akka.actor.ActorRef
import breeze.linalg.DenseVector
import gossiper.SingleMeanGossiper
import message._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

// todo: broken system....
class WeightedGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  var diffuseMat: Map[ActorRef, Double] = Map()
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()
  var nextRndMailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()

  override def work(inValidState: Boolean, neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = common(gossiper) orElse {
    case InitMessage(nbs) =>
      val neighbors = nbs + (name -> self)
      setDiffuseMatrix(neighbors)
      context become work(inValidState, neighbors, gossiper.bumpRound())

    case StartMessage(_) =>
      context become work(inValidState, neighbors, gossip(neighbors.values.head, gossiper, false))

    case PushSignal =>
      context become work(inValidState, neighbors, gossip(neighbors.values.head, gossiper, false))

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
          context become work(inValidState, neighbors, newState)
          self ! StopMessage
        } else {
          mailbox = nextRndMailbox
          nextRndMailbox = new ListBuffer()
          context become work(inValidState, neighbors, newState.bumpRound())
          self ! PushSignal
        }
      }

    case StopMessage =>
      context become work(inValidState, neighbors, gossip(neighbors.values.head, gossiper.wrap(), false))

  }

  override def gossip(target: ActorRef, gossiper: SingleMeanGossiper, isResend: Boolean): SingleMeanGossiper = {
    for ((k, v) <- diffuseMat.iterator) {
      k ! WeightedPushMessage(gossiper.data * v, gossiper.roundCount)
    }
    gossiper.copy(messageCount = gossiper.messageCount + diffuseMat.size)
  }

  private def setDiffuseMatrix(nbs: Map[String, ActorRef]) {
    val numNeighbors = nbs.size
    diffuseMat = nbs.valuesIterator.map(_ -> 1.0 / numNeighbors) toMap
  }

  override def waitTime = (rnd.nextInt(10) * 10) millis
}
