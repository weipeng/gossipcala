package gossiper

import breeze.linalg.DenseVector
import breeze.numerics._
import util.Config


case class SingleMeanGossiper private(override val data: DenseVector[Double],
                                      override val status: GossiperStatus.Value,
                                      override val roundCount: Int = 0,
                                      override val wastedRoundCount: Int = 0,
                                      override val invalidMessageCount: Int = 0,
                                      override val messageCount: Int = 0,
                                      convergenceCount: Int = 0,
                                      lastMetric: Double = 0.0) extends AggregateGossiper {

  private val stoppingThreshold: Int = Config.algorithm.stoppingThreshold
  private val errorBound: Double = Config.algorithm.errorBound
  private val wastedRoundThreshold: Double = Config.algorithm.wastedRoundThreshold

  override def wrap(): SingleMeanGossiper = copy(status = GossiperStatus.COMPLETE)

  override def toStop(): Boolean = convergenceCount >= stoppingThreshold

  override def estimate(): Double = data(1) / data(0)

  override def compareData(): SingleMeanGossiper = {
    val currentMetric = estimate()
    if (abs(currentMetric / lastMetric - 1) < errorBound) {
      copy(lastMetric = currentMetric, convergenceCount = convergenceCount + 1)
    } else {
      copy(lastMetric = currentMetric, convergenceCount = 0)
    }
  }

  override def update(value: Double): SingleMeanGossiper = {
    data(1) = (data(1) + value) / 2.0
    val wasteQuantity = if (abs(data(1) / value) - 1 <= wastedRoundThreshold) 1 else 0
    copy(wastedRoundCount = wastedRoundCount + wasteQuantity)
  }

  def bumpRound(): SingleMeanGossiper = copy(roundCount = roundCount + 1)

  def bumpMessage(): SingleMeanGossiper = copy(messageCount = messageCount + 1)

  def bumpInvalidMessage(): SingleMeanGossiper = copy(invalidMessageCount = invalidMessageCount + 1)

  def isWasted(value: Double): Boolean = abs(value - data(1) / data(0)) <= wastedRoundThreshold 
}

object SingleMeanGossiper {
  def apply(initData: Double): SingleMeanGossiper = {
    val raw = SingleMeanGossiper(
      DenseVector(1.0, initData),
      GossiperStatus.ACTIVE
    )
    raw.copy(lastMetric = raw.estimate())
  }
}
