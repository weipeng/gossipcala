package actor

import akka.actor.{ActorLogging, ActorRef, Props}
import breeze.linalg._
import gossiper.SingleMeanGossiper
import message._
import scala.Vector

import scala.concurrent.duration._
import scala.language.postfixOps
import com.typesafe.scalalogging.LazyLogging


class WeightedGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper)
  extends GossiperActorTrait[Double, SingleMeanGossiper, WeightExtraState] with ActorLogging with LazyLogging {

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    wState: WeightExtraState): Receive = common(gossiper) orElse {
    case InitMessage(nbs) =>
      val neighbors = nbs + (name -> self)
      context become work(neighbors,
        gossiper.bumpRound(),
        wState.copy(
          diffuseMatrix = neighbors.values.map(_ -> 1.0 / neighbors.size).toMap)
        )

    case StartMessage(_) =>
      context become work(neighbors, gossip(gossiper, wState), wState)

    case WeightedPushMessage(data, roundCount) =>
      log.debug(s"$name in ${gossiper.roundCount} receive $data from ${GossiperActorTrait.extractName(sender)}")
      val mailBoxState = wState.mailbox :+ data
      if (sender == self) {
        val newState = update(gossiper, mailBoxState).compareData()

        if (newState.toStop()) {
          context become work(neighbors, newState, 
                              wState.copy(mailbox = Vector.empty))
          self ! StopMessage
        } else {
          context become work(neighbors, newState.bumpRound(), 
                              wState.copy(mailbox = Vector.empty))
          self ! StartMessage(None)
        }
      } else {
        context become work(neighbors, gossiper, 
                            wState.copy(mailbox = mailBoxState))
      }

    case StopMessage =>
      context become work(neighbors, gossiper.wrap(), wState)
  }

  private def gossip(gossiper: SingleMeanGossiper, 
                     wState: WeightExtraState): SingleMeanGossiper = {
    val diffuseMat = wState.diffuseMatrix
    for ((x, v) <- diffuseMat) { 
      if (x != self) {
        log.debug(s"$name sends to $x")
        x ! WeightedPushMessage(gossiper.data * v, gossiper.roundCount)
      }
    }
  
    sendSelfWithDelay(WeightedPushMessage(gossiper.data * diffuseMat(self), gossiper.roundCount))
    gossiper.copy(messageCount = gossiper.messageCount + diffuseMat.size)
  }
  
  private def update(gossiper: SingleMeanGossiper, 
                     mailbox: Vector[DenseVector[Double]]): SingleMeanGossiper = {
    val data = sum(mailbox)
    val isWasted = gossiper.isWasted(data(1) / data(0))
    val wasteQuantity = if (isWasted) 1 else 0
    if (isWasted) logger.warn(s"${name}+++${gossiper.roundCount}")
    gossiper.copy(data = data,
                  wastedRoundCount = gossiper.wastedRoundCount + wasteQuantity)
  }

  override val defaultExtraState = WeightExtraState(Map.empty, Vector.empty)
  override val waitTime: FiniteDuration = 100 millis
}

object WeightedGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new WeightedGossiper(name, gossiper))
}
