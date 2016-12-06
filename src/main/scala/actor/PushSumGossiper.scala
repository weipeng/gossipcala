package actor

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import breeze.linalg._
import gossiper.SingleMeanGossiper
import message._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

class PushSumGossiper(override val name: String,
                      override val gossiper: SingleMeanGossiper)
  extends GossiperActorTrait[Double, SingleMeanGossiper, EmptyState.type] {
  lazy val log = Logging(context.system, this)
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer
  val weight = 0.5

  override def work(neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper,
                    extraState: EmptyState.type = EmptyState): Receive = common(gossiper) orElse {

    case InitMessage(nbs) =>
      context become work(nbs, gossiper)

    case PushSumMessage(value) =>
      if (sender == self) {
        val newState = if (mailbox.isEmpty) {
                         mailbox.append(value)
                         updateGossiper(gossiper).compareData()
                       }
                       else gossiper.copy(data = value).compareData()

        if (newState.toStop) {
          self ! StopMessage
        } else {
          sendSelfWithDelay(StartMessage(None))
        }
        context become work(neighbors, newState)
      } else {
        log.info(s"${self.toString} receives a PUSH message from ${sender.toString} ${value}")
        mailbox.append(value)
        context become work(neighbors, gossiper)
      }
  
    case StopMessage =>
      context become work(neighbors, gossiper.wrap())

    case StartMessage(_) =>
      val neighbor = nextNeighbour(neighbors, null)
      context become work(neighbors, gossip(neighbor, gossiper))

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def nextNeighbour(neighbors: Map[String, ActorRef], banNeighbor: Option[ActorRef]): ActorRef = {
    val neighbours = neighbors.values.toArray[ActorRef]
    neighbours(rnd.nextInt(neighbours.length))
  }

  private def gossip(target: ActorRef, gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    self ! msg
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushSumMessage, SingleMeanGossiper) =
    (PushSumMessage(gossiper.data * weight), gossiper.bumpMessage())

  def updateGossiper(gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val isWasted = gossiper.isWasted(gossiper.data(1) / gossiper.data(0))
    val wasteQuantity = if (isWasted) 1 else 0
    val gossiperCopy = gossiper.copy(data = sum(mailbox), 
                                     wastedRoundCount = gossiper.wastedRoundCount + wasteQuantity)
    mailbox = new ListBuffer()
    gossiperCopy
  }

  override val defaultExtraState = EmptyState
  override val waitTime: FiniteDuration = 50 millis
}

object PushSumGossiper {
  def props(name: String, gossiper: SingleMeanGossiper) = Props(new PushSumGossiper(name, gossiper))
}
