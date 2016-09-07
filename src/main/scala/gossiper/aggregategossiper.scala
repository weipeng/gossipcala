package gossiper

import breeze.linalg.DenseVector

trait AggregateGossiper[T] {
  val data: DenseVector[T]
  val status: GossiperStatus.Value = GossiperStatus.ACTIVE
  val roundCount: Int = 0
  val messageCount: Int = 0
  def wrap(): AggregateGossiper[T]
  def toStop(): Boolean
  def estimate(): T
  def compareData(): AggregateGossiper[T]
  def update(value: T): AggregateGossiper[T]
}