import akka.util.Timeout
import breeze.linalg.DenseVector
import gossiper._
import graph.GraphFileReader
import simulation.Simulation

import scala.concurrent.duration._
import scala.language.postfixOps


object Main {
  def main(args: Array[String]): Unit = {
    //Simulation.batchSim()
    sim()
    //sim2()
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

    val data = Array[Double](233, 21, 53, 402)

    val simpleGraph = GraphFileReader("dummy").parseJson(graphTemplate)

    Simulation.simWithRepetition(1, DenseVector(data), "dummy", simpleGraph, GossipType.WEIGHTED)
  }

  def graphTemplate: String = {
    """{"directed":false,"index":0,"links":[{"source":0,"target":1},{"source":0,"target":2},{"source":1,"target":3}],"multigraph":false,"graph":{"name":"test_simple_graph"},"meanSharedNeighbors":-1,"var_degree":-1,"mean_degree":-1,"nodes":[{"id":0},{"id":1},{"id":2},{"id":3}],"order":4}"""
  }

  def sim2(): Unit = {
    implicit val timeout = Timeout(1 seconds)
    val graph: String = {
      """{"directed":false,"index":0,"links":[{"source":0,"target":1}, {"source":0,"target":2}, {"source":1,"target":2}],"multigraph":false,"graph":{"name":"test_simple_graph"},"meanSharedNeighbors":-1,"var_degree":-1,"mean_degree":-1,"nodes":[{"id":0},{"id":1},{"id":2}],"order":3}"""
    }
    val data = Array[Double](-10, 50, 100)

    val simpleGraph = GraphFileReader("dummy").parseJson(graph)

    Simulation.simWithRepetition(1, DenseVector(data), "dummy", simpleGraph, GossipType.PUSHSUM)
  }
}
