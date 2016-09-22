package util

import breeze.linalg.DenseVector
import breeze.numerics._
import breeze.stats.{mean, variance}
import message.NodeState

import scala.collection.immutable.Map

/**
  * Author: yanyang.wang
  * Date: 22/09/2016
  */
case class ResultAnalyser(dataMean: Double, graphOrder: Int, nodeStates: List[NodeState]) {
  def analysis(): Map[String, String] = {
    val rounds = DenseVector.zeros[Double](graphOrder)
    val messages = DenseVector.zeros[Double](graphOrder)
    val wastedRnds = DenseVector.zeros[Double](graphOrder)
    val errors = DenseVector.zeros[Double](graphOrder)
    for ((n, i) <- nodeStates.zipWithIndex) {
      rounds(i) = n.roundCount
      wastedRnds(i) = n.wastedRoundCount
      messages(i) = n.messageCount
      errors(i) = n.estimate / dataMean - 1
    }

    Map(
      "Mean L1-absolute error" -> mean(abs(errors)).toString,
      "Var L1-absolute error" -> variance(abs(errors)).toString,
      "Mean L2 error" -> mean(pow(errors, 2)).toString,
      "Var L2 error" -> variance(pow(errors, 2)).toString,
      "Mean rounds" -> mean(rounds).toString,
      "Var rounds" -> variance(rounds).toString,
      "Mean wasted rounds" -> mean(wastedRnds).toString,
      "Var wasted rounds" -> variance(wastedRnds).toString,
      "Mean messages" -> mean(messages).toString,
      "Var messages" -> variance(messages).toString
    )
  }
}
