package gossiper

import breeze.linalg._
import breeze.numerics._
import breeze.math._
import akka.actor.{Actor,Props,ActorRef}
import scala.util.Random
import scala.collection.mutable.Map
import message._


class PushPullGossiper(override val name: String, var inData: Double) 
    extends SingleMeanGossiper(name, inData) {

    val rnd = new Random 
    override def receive: Receive = {
        case InitMessage(_neighbors) => 
            this.setNeighbors(_neighbors)

        case PushMessage(_data) =>
            val msg = makePullMessage
            sender ! msg
            this.update(_data)
            this.compareData
            if (this.toStop)
                self ! StopMessage
            this.gossip

        case PullMessage(_data) =>
            this.update(_data) 

        case StopMessage =>
            this.wrap

        case StartMessage =>
            this.gossip

        case KillMessage =>
            context.stop(self)

        case AskStateAndEstimateMessage =>
            sender ! Tuple2[Boolean, Double](toStop, getEstimate)        
    
        case _ =>
            println("what the heck")
    }

    override def gossip() {
        val nbs = neighbors.values.toArray
        val neighbor = nbs(rnd.nextInt(neighbors.size))
        val msg = makePushMessage
        neighbor ! msg
    }

    def update(data: Double) {
        this.data(1) = (this.data(1) + data) / 2.0
    }

    def makePushMessage = {
        PushMessage(data(1))
    }

    def makePullMessage = {
        PullMessage(data(1))
    }

}
