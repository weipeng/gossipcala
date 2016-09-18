import actor.PushPullGossiper
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import gossiper._
import graph.GraphFileReader
import message._

import scala.collection.immutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


object Simulation {
  def main(args: Array[String]): Unit = {
    fileReadTest()
  }

  def fileReadTest() = {
    val graph = GraphFileReader("sf_200_10_0.data.gz").readGraph()
    //println(graph.nodes)
    println(graph.nodes(2).links)
    graph.nodes(2).links foreach { n =>
      println(n.id.getClass.getName)
    }
    //println(graph)
  }

  def sim() {
    implicit val timeout = Timeout(1 seconds)

    val system = ActorSystem("Gossip")

    val numNodes = 3
    val members = new ArrayBuffer[ActorRef]
    val data = Array[Double](233, 21, 53)
    val dataSum = data.sum
    (0 until numNodes).foreach { i =>
      members.append(
        system.actorOf(Props(new PushPullGossiper(s"node$i", SingleMeanGossiper(data(i)))), name = "node" + i)
      )
    }

    members(0) ! InitMessage(Map("node1" -> members(1),
      "node2" -> members(2)))
    members(1) ! InitMessage(Map("node0" -> members(0)))
    members(2) ! InitMessage(Map("node1" -> members(0)))

    members.foreach { m => m ! StartMessage }

    var flag = false
    while (!flag) {
      Thread.sleep(200)

      val futures = members map { m =>
        (m ? CheckState).mapTo[NodeState]
      }
      val futureList = Future.sequence(futures)
      futureList map { x =>
        flag = x.forall(_.status == GossiperStatus.COMPLETE)
      }

      futureList map { x =>
        println(x + "AAAAAAAAAAAAAAAAAAH " + flag)
        for ((m, i) <- members.zipWithIndex) {
          println(m.path.name + " " + abs(x(i).estimate / (dataSum / 3) - 1))
        }
        println("Average " + dataSum / 3)
      }

      if (flag)
        system.terminate
    }
  }
}
