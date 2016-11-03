package gossiper

import breeze.linalg.DenseVector

trait AggregateGossiper {
  type Data = Double
  val data: DenseVector[Data]
  val status: GossiperStatus.Value
  val roundCount: Int
  val wastedRoundCount: Int
  val busyMessageCount: Int
  val messageCount: Int
  def wrap(): AggregateGossiper
  def toStop(): Boolean
  def estimate(): Data
  def compareData(): AggregateGossiper
  def update(value: Data): AggregateGossiper
}
