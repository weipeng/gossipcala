package gossiper

import breeze.linalg.DenseVector
import breeze.numerics._
import util.Configs.config


case class SingleMeanGossiper private(override val data: DenseVector[Double],
                                      override val status: GossiperStatus.Value,
                                      override val roundCount: Int = 0,
                                      override val messageCount: Int = 0,
                                      override val wastedRoundCount: Int = 0,
                                      convergenceCount: Int = 0,
                                      lastMetric: Double = 0.0) extends AggregateGossiper[Double] {

  private val stoppingThreshold: Int = config.getInt("stoppingThreshold")
  private val errorBound: Double = config.getDouble("errorBound")
  private val wastedRoundThreshold: Double = config.getDouble("wastedRoundThreshold")

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
    if (abs(data(1) - value) <= wastedRoundThreshold) {
        copy(wastedRoundCount = wastedRoundCount + 1)
    }   
    data(1) = (data(1) + value) / 2.0
    this
  }

  def bumpRound(): SingleMeanGossiper = copy(roundCount = roundCount + 1)
}

object SingleMeanGossiper {
  def apply(initData: Double): SingleMeanGossiper = SingleMeanGossiper(
    DenseVector(1.0, initData),
    GossiperStatus.ACTIVE
  )
}
