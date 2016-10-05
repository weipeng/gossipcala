package actor

import akka.actor.ActorRef
import akka.event.Logging
import gossiper.SingleMeanGossiper
import message._
import scala.collection.mutable.ListBuffer
import scala.util.Random
import breeze.linalg._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


class PushSumGossiper(override val name: String,
                      override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  lazy val rnd = new Random(System.currentTimeMillis)
  lazy val log = Logging(context.system, this)
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer
  val weight = 0.5

  override def work(neighbors: Map[String, ActorRef],   
                    gossiper: SingleMeanGossiper): Receive = {

    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper)

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
          context.system.scheduler.scheduleOnce(100 milliseconds) {
            self ! PushSignal
          }
        }
        context become work(neighbors, newState)
      } else {
        //log.info(s"${self.toString} receives a PUSH message from ${sender.toString} ${value}")
        mailbox.append(value)
        context become work(neighbors, gossiper)
      }

    case PushSignal =>
      context become work(neighbors, gossip(neighbors, gossiper))
  
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

  override def gossip(neighbors: Map[String, ActorRef], 
                      gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val nbs = neighbors.values.toArray
    val neighbor = nbs(rnd.nextInt(neighbors.size))
    val (msg, state) = makePushMessage(gossiper)
    neighbor ! msg
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
}
