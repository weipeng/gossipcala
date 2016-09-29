package util

import breeze.linalg.DenseVector
import breeze.numerics._
import breeze.stats.{mean, variance}
import message.NodeState

import scala.collection.immutable.ListMap

/**
  * Author: yanyang.wang
  * Date: 22/09/2016
  */
case class ResultAnalyser(dataMean: Double, graphOrder: Int, nodeStates: List[NodeState]) {
  def analyse(): Map[String, String] = {
    val rounds = DenseVector.zeros[Double](graphOrder)
    val messages = DenseVector.zeros[Double](graphOrder)
    val wastedRounds = DenseVector.zeros[Double](graphOrder)
    val errors = DenseVector.zeros[Double](graphOrder)
    val effectiveRounds = rounds - wastedRounds
    for ((n, i) <- nodeStates.zipWithIndex) {
      rounds(i) = n.roundCount
      wastedRnds(i) = n.wastedRoundCount
      messages(i) = n.messageCount
      errors(i) = n.estimate / dataMean - 1
    }

    ListMap(
      "Mean L1-absolute Error" -> mean(abs(errors)).toString,
      "Var L1-absolute Error" -> variance(abs(errors)).toString,
      "Mean L2 Error" -> mean(pow(errors, 2)).toString,
      "Var L2 Error" -> variance(pow(errors, 2)).toString,
      "Mean Rounds" -> mean(rounds).toString,
      "Var Rounds" -> variance(rounds).toString,
      "Mean Effective Rounds" -> mean(effectiveRounds).toString,
      "Var Effective Rounds" -> variance(effectiveRounds).toString,
      "Mean Wasted Rounds" -> mean(wastedRounds).toString,
      "Var Wasted Rounds" -> variance(wastedRounds).toString,
      "Mean Messages" -> mean(messages).toString,
      "Var Messages" -> variance(messages).toString
    )
  }
}
