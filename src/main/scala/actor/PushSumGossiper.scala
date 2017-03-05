package actor

import akka.actor.{ActorLogging, ActorRef, Props}
import breeze.linalg._
import gossiper.SingleMeanGossiper
import message._

import scala.concurrent.duration._
import scala.language.postfixOps
import com.typesafe.scalalogging.LazyLogging


class PushSumGossiper(override val name: String,
                      override val gossiper: SingleMeanGossiper)
  extends BinaryGossiperTrait[Double, SingleMeanGossiper, PushSumExtraState] with ActorLogging with LazyLogging {
  private val weight = 0.5

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    sumState: PushSumExtraState): Receive = common(gossiper) orElse {

    case InitMessage(nbs) =>
      context become work(nbs, gossiper, sumState)

    case PushSumMessage(value) =>
      val newMailbox = sumState.mailBox :+ value
      if (sender == self) {
        val newState = 
          if (newMailbox.size > 1) update(gossiper, newMailbox).compareData()
          else gossiper.copy(data = value, 
                             wastedRoundCount = gossiper.wastedRoundCount + 1).compareData()

        if (newState.toStop()) {
          self ! StopMessage
        } else {
          sendSelfWithDelay(StartMessage(None))
        }
        context become work(neighbors, newState, sumState.copy(mailBox = scala.Vector.empty))
      } else {
        log.info(s"${GossiperActorTrait.extractName(self)} receives a PUSH message from ${GossiperActorTrait.extractName(sender)} $value")
        context become work(neighbors, gossiper, sumState.copy(newMailbox))
      }
  
    case StopMessage =>
      context become work(neighbors, gossiper.wrap(), sumState)

    case StartMessage(t) =>
      val neighbor = t.getOrElse(nextNeighbor(neighbors, None))
      context become work(neighbors, gossip(neighbor, gossiper), sumState)

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def gossip(target: ActorRef, gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    self ! msg
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushSumMessage, SingleMeanGossiper) =
    (PushSumMessage(gossiper.data * weight), gossiper.bumpMessage())

  private def update(gossiper: SingleMeanGossiper, mailbox: scala.Vector[DenseVector[Double]]): SingleMeanGossiper = {
    val newData = sum(mailbox)
    val isWasted = gossiper.isWasted(newData(1) / newData(0))
    val wasteQuantity = if (isWasted) 1 else 0
    //if (isWasted) logger.warn(s"${name}+++${gossiper.roundCount}")
    gossiper.copy(data = newData, 
                  wastedRoundCount = gossiper.wastedRoundCount + wasteQuantity)
  }

  override val defaultExtraState = PushSumExtraState(scala.Vector.empty)
  override val waitTime: FiniteDuration = 450 millis
}

object PushSumGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new PushSumGossiper(name, gossiper))
}
