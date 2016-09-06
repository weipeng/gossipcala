package gossiper

import message._

import scala.util.Random


class PushPullGossiper(override val name: String, var inData: Double)
  extends SingleMeanGossiper(name, inData) {

  val rnd = new Random

  override def receive: Receive = {
    case InitMessage(nbs) =>
      neighbors = nbs
      rounds += 1

    case PushMessage(d) =>
      sender ! makePullMessage()
      update(d)

    case PullMessage(d) =>
      update(d)
      compareData()
      if (toStop) {
        self ! StopMessage
      } else {
        gossip()
      }

    case StopMessage =>
      wrap()

    case StartMessage =>
      gossip()

    case KillMessage =>
      context.stop(self)

    case CheckState =>
      sender ! NodeState(status, rounds, getEstimate())

    case msg =>
      println(s"Unexpected message $msg received")
  }

  override def gossip() {
    val nbs = neighbors.values.toArray
    val neighbor = nbs(rnd.nextInt(neighbors.size))
    neighbor ! makePushMessage()
    rounds += 1
  }

  def update(d: Double) {
    data(1) = (data(1) + d) / 2.0
  }

  private def makePushMessage() = PushMessage(data(1))

  private def makePullMessage() = PullMessage(data(1))

}
