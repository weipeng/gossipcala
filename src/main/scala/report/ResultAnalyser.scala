package report

import breeze.linalg.DenseVector
import breeze.numerics._
import breeze.stats.{mean, variance}
import gossiper.GossipType
import graph.Graph
import message.NodeState

/**
  * Author: yanyang.wang
  * Date: 22/09/2016
  */
case class ResultAnalyser(dataMean: Double,
                          nodeStates: List[NodeState],
                          simCounter: Int,
                          gossipType: GossipType.Value,
                          graph: Graph) {
  def analyse(): Report = {
    val graphOrder = graph.order
    val meanSharedNeighbors = graph.meanSharedNeighbors
    val rounds = DenseVector.zeros[Double](graphOrder)
    val messages = DenseVector.zeros[Double](graphOrder)
    val busyMessages = DenseVector.zeros[Double](graphOrder)
    val wastedRounds = DenseVector.zeros[Double](graphOrder)
    val errors = DenseVector.zeros[Double](graphOrder)
    for ((n, i) <- nodeStates.zipWithIndex) {
      rounds(i) = n.roundCount
      wastedRounds(i) = n.wastedRoundCount
      messages(i) = n.messageCount
      busyMessages(i) = n.busyMessageCount
      errors(i) = n.estimate / dataMean - 1
    }
    val effectiveRounds = rounds - wastedRounds

    Report(
      graphOrder = graphOrder,
      graphType = graph.graphType,
      graphMeanDegree = graph.meanDegree,
      graphIndex = graph.index,
      simCounter = simCounter,
      gossipType = gossipType,
      meanL1AbsoluteError = mean(abs(errors)),
      varL1AbsoluteError = variance(abs(errors)),
      meanL2Error = mean(pow(errors, 2)),
      varL2Error = variance(pow(errors, 2)),
      meanRounds = mean(rounds),
      varRounds = variance(rounds),
      meanEffectiveRounds = mean(effectiveRounds),
      varEffectiveRounds = variance(effectiveRounds),
      meanWastedRounds = mean(wastedRounds),
      varWastedRounds = mean(messages),
      meanMessages = mean(messages),
      varMessages = variance(messages),
      meanBusyMessages = mean(busyMessages),
      varBusyMessages = variance(busyMessages),
      meanSharedNeighbors = meanSharedNeighbors
    )
  }
}
