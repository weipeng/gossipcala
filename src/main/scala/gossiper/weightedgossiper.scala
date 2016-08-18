package gossiper

import akka.actor.ActorRef
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import breeze.linalg.DenseVector
import message._
import scala.language.postfixOps


class WeightedGossiper(override val name: String, var inData: Double) extends 
      SingleMeanGossiper(name, inData) {
    
    var diffuseMat: Map[ActorRef, Double] = Map()
    var mailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()
    var nextRndMailbox: ListBuffer[DenseVector[Double]] = new ListBuffer()

    def setDiffuseMatrix() {
        val numNeighbors = neighbors.size
        diffuseMat = neighbors.valuesIterator.map(_ -> 1.0 / numNeighbors) toMap           
    }

    override def receive: Receive = {
        case InitMessage(_nbs) =>
            neighbors = _nbs 
            neighbors += (name -> self)
            setDiffuseMatrix
            rounds += 1
            
        case StartMessage =>
            this.gossip

        case PushSignal =>
            this.gossip

        case WeightedPushMessage(_data, _rounds) =>
            /*if (_rounds == rounds+1) {
                nextRndMailbox.append(_data)
            } else if (_rounds == rounds) {
                mailbox.append(_data)
            }*/
            mailbox.append(_data)
            
            if (mailbox.size == neighbors.size) {
                data = mailbox.fold(DenseVector(0.0, 0.0))((x, y) => x + y)
                compareData
                if (toStop) {
                    self ! StopMessage 
                } 

                mailbox = nextRndMailbox
                nextRndMailbox = new ListBuffer()
                rounds += 1
                self ! PushSignal       
            } 

        case StopMessage =>
            this.wrap

        case KillMessage =>
            context.stop(self)

        case AskStateAndEstimateMessage =>
            sender ! Tuple2[Boolean, Double](toStop, getEstimate)
            
    }
    
    override def gossip() {
        for ((k, v) <- diffuseMat.iterator) {
            k ! WeightedPushMessage(data * v, rounds)
        } 
        messages += diffuseMat.size
    }    

}
