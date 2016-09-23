import actor.PushPullGossiper
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import gossiper._
import graph.GraphFileReader
import message._
import simulation.Simulation
import util.{ReportGenerator, ResultAnalyser}

import scala.collection.immutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


object Main {
  def main(args: Array[String]): Unit = {
    Simulation.batchSim()
    //sim()
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
    val data = Array[Double](233, 21, 53)
    val dataSum = data.sum
    val dataMean = dataSum / numNodes
    val members = (0 until numNodes).map { i =>
        system.actorOf(Props(new PushPullGossiper(s"node$i", SingleMeanGossiper(data(i)))), name = "node" + i)
    }.toList

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
          println(m.path.name + " " + abs(x(i).estimate / dataMean - 1))
        }
        println("Average " + dataSum / 3)
      }

      if (flag) {
        futureList map { nodeStates =>
          val rawReport = ResultAnalyser(dataMean, 3, nodeStates).analyse()
          println(rawReport)
          nodeStates.foreach(println)
        }
        system.terminate
      }
    }
  }
}
