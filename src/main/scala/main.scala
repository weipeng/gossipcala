import actor.PushSumGossiper
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import gossiper._
import graph.{JsonGraph, Node, Graph, GraphFileReader}
import message._
import report.ResultAnalyser

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


object Main {
  def main(args: Array[String]) {
    //Simulation.batchSim()
    sim()
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

    val numNodes = 4
    val data = Array[Double](233, 21, 53, 402)

    val simpleGraph = GraphFileReader("dummy").parseJson(graphTemplate)
    val dataSum = data.sum
    val dataMean = dataSum / numNodes
    val members = (0 until numNodes).map { i =>
        system.actorOf(Props(new PushSumGossiper(s"node$i", SingleMeanGossiper(data(i)))), name = "node" + i)
    }.toList

    members(0) ! InitMessage(Map("node1" -> members(1), "node2" -> members(2)))
    members(1) ! InitMessage(Map("node0" -> members(0), "node3" -> members(3)))
    members(2) ! InitMessage(Map("node0" -> members(0)))
    members(3) ! InitMessage(Map("node1" -> members(1)))

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
        println("Average " + dataMean)
      }

      if (flag) {
        futureList map { nodeStates =>
          val report = ResultAnalyser(dataMean, nodeStates, 1, "unknown", simpleGraph).analyse()
          println(report.printString)
          nodeStates.foreach(println)
        }
        system.terminate
      }
    }
  }

  def graphTemplate: String = {
    """{"directed":false,"index":0,"links":[{"source":0,"target":1},{"source":0,"target":2},{"source":1,"target":3}],"multigraph":false,"graph":{"name":"test_simple_graph"},"meanSharedNeighbors":-1,"var_degree":-1,"mean_degree":-1,"nodes":[{"id":0},{"id":1},{"id":2},{"id":3}],"order":4}"""
  }
}
