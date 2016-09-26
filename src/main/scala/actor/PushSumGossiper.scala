package actor

import akka.actor.ActorRef
import akka.event.Logging
import gossiper.SingleMeanGossiper
import message._
import scala.collection.mutable.ListBuffer
import scala.util.Random
import breeze.linalg._
//import breeze.math._
//import breeze.numerics._


class PushSumGossiper(override val name: String,
                       override val gossiper: SingleMeanGossiper) extends GossiperActorTrait[Double, SingleMeanGossiper] {

  lazy val rnd = new Random(System.currentTimeMillis)
  lazy val log = Logging(context.system, this)
  var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()   
  val weight = 0.5

  override def work(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): Receive = {
    case InitMessage(neighbors) =>
      context become work(neighbors, gossiper)

    case PushSumMessage(value) =>
      if (sender == self) {
        val tmpGossiper = if (mailbox.size > 1) updateGossiper(gossiper, value) 
                          else gossiper
        self ! PushSignal
        context become work(neighbors, tmpGossiper)
      } else {
        // log.info(s"${self.toString} receives a PUSH message from ${sender.toString}")
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

  override def gossip(neighbors: Map[String, ActorRef], gossiper: SingleMeanGossiper): SingleMeanGossiper = {
    val nbs = neighbors.values.toArray
    val neighbor = nbs(rnd.nextInt(neighbors.size))
    val (msg, state) = makePushMessage(gossiper)
    neighbor ! msg
    self ! msg 
    state.bumpRound()
  }

  private def makePushMessage(gossiper: SingleMeanGossiper): (PushSumMessage, SingleMeanGossiper) =
    (PushSumMessage(gossiper.data * weight), gossiper.bumpMessage())

  def updateGossiper(gossiper: SingleMeanGossiper, value: DenseVector[Double]): SingleMeanGossiper = {
    val gossiperCopy = gossiper.copy(data = sum(mailbox))
    mailbox = new ListBuffer()
    gossiperCopy
  }
}
