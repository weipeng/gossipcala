package gossiper

import akka.actor.{Actor,Props,ActorRef}
import breeze.linalg.DenseVector
import breeze.math._
import breeze.numerics._
import scala.collection.immutable.Map


class SingleMeanGossiper(val name: String, var inputData: Double) extends Actor {

    var neighbors = Map[String, ActorRef]()
    var data = DenseVector[Double](1.0, inputData)
    var oldMetric: Double = 0.0
    var convergence_counter = 0
    
    val errorBound = 0.001
    val stoppingTimes = 15
    var estimate = 0.0
    var status = "active"

    var rounds = 0
    var messages = 0

    def wrap() { 
        estimate = getEstimate 
        status = "finished"
    }

    def toStop: Boolean = (convergence_counter >= stoppingTimes) 

    def getEstimate(): Double = data(1) / data(0)

    def compareData {
        val tmpData = getEstimate
        if (abs(tmpData/oldMetric - 1) < errorBound) {
            convergence_counter += 1
        } else {
            convergence_counter = 0
        }
        oldMetric = tmpData
    }
    
    def setNeighbors(nbs: Map[String, ActorRef]) {
        neighbors = nbs
    }
    
    def update() {}
    
    def gossip() {}

    override def receive: Receive = {
        case any => println(any) 
    }
}
