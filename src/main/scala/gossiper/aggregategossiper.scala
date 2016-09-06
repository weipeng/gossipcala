package gossiper

import akka.actor.{Actor, Props, ActorRef}
import breeze.linalg.DenseVector
import breeze.math._
import breeze.numerics._
import scala.collection.immutable.Map


class SingleMeanGossiper(inputData: Double) {

  var neighbors: Map[String, ActorRef] = Map.empty
  var data: DenseVector[Double] = DenseVector(1.0, inputData)
  var oldMetric: Double = 0.0
  var convergence_counter: Int = 0

  val errorBound: Double = 0.001
  val stoppingTimes: Int = 15
  var estimate: Double = 0.0
  var status: NodeStatus.Value = NodeStatus.ACTIVE

  var rounds: Int = 0
  var messages = 0

  def wrap() {
    estimate = getEstimate()
    status = NodeStatus.COMPLETE
  }

  def toStop: Boolean = convergence_counter >= stoppingTimes

  def getEstimate(): Double = data(1) / data(0)

  protected def compareData(): Unit = {
    val tmpData = getEstimate()
    if (abs(tmpData / oldMetric - 1) < errorBound) {
      convergence_counter += 1
    } else {
      convergence_counter = 0
    }
    oldMetric = tmpData
  }

  def update() {}

  def gossip() {}

  override def receive: Receive = {
    case any => println(any)
  }
}
