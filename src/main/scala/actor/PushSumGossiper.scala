package actor

import akka.actor.ActorRef
import akka.event.Logging
import breeze.linalg._
import gossiper.SingleMeanGossiper
import message._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

// todo: broken....
class PushSumGossiper(override val name: String,
                      override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {
  lazy val log = Logging(context.system, this)
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer
  val weight = 0.5

  override def work(inValidState: Boolean,
                    neighbors: Map[String, ActorRef],
                    gossiper: SingleMeanGossiper): Receive = common(gossiper) orElse {

    case InitMessage(neighbors) =>
      context become work(inValidState, neighbors, gossiper)

    case PushSumMessage(value) =>
      if (sender == self) {
        val newState = if (mailbox.size > 0) {
                         mailbox.append(value)
                         updateGossiper(gossiper, value).compareData
                       }
                       else gossiper.copy(data = value).compareData
        
        if (newState.toStop) {
          self ! StopMessage
        } else {
          context.system.scheduler.scheduleOnce(50 milliseconds) {
            self ! PushSignal
          }
        }
        context become work(inValidState, neighbors, newState)
      } else {
        log.info(s"${self.toString} receives a PUSH message from ${sender.toString} ${value}")
        mailbox.append(value)
        context become work(inValidState, neighbors, gossiper)
      }

    case PushSignal =>
      context become work(inValidState, neighbors, gossip(neighbors.values.head, gossiper, false))
  
    case StopMessage =>
      context become work(inValidState, neighbors, gossiper.wrap())

    case StartMessage(_) =>
      context become work(inValidState, neighbors, gossip(neighbors.values.head, gossiper, false))

    case msg =>
      println(s"Unexpected message $msg received")
  }

  private def nextNeighbour(neighbors: Map[String, ActorRef], banNeighbor: Option[ActorRef]): ActorRef = {
    val neighbours = neighbors.values.toArray[ActorRef]
    neighbours(rnd.nextInt(neighbours.length))
  }

  override def gossip(target: ActorRef, gossiper: SingleMeanGossiper, isResend: Boolean): SingleMeanGossiper = {
    val (msg, state) = makePushMessage(gossiper)
    target ! msg
    self ! msg 
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushSumMessage, SingleMeanGossiper) =
    (PushSumMessage(gossiper.data * weight), gossiper.bumpMessage())

  def updateGossiper(gossiper: SingleMeanGossiper, 
                     value: DenseVector[Double]): SingleMeanGossiper = {
    val data = sum(mailbox)
    val isWasted = gossiper.isWasted(gossiper.data(1) / gossiper.data(0))
    val wasteQuantity = if (isWasted) 1 else 0
    val gossiperCopy = gossiper.copy(data = sum(mailbox), 
                                     wastedRoundCount = gossiper.wastedRoundCount + wasteQuantity)
    mailbox = new ListBuffer()
    gossiperCopy
  }

  override def waitTime = (rnd.nextInt(10) * 10) millis
}
