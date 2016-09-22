package simulation

import actor.PushPullGossiper
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import breeze.stats._
import gossiper._
import graph.GraphFileReader
import message._
import util.{ResultAnalyser, DataReader, ReportGenerator}

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


object Simulation {

  def sim(numNodes: Int, 
          data: DenseVector[Double], 
          repeatition: Int = 1,
          verbose: Boolean = false) {

    implicit val timeout = Timeout(1 seconds)

    val dataMean = mean(data)
    val graph = GraphFileReader(s"sf_${numNodes}_10_0.data.gz").readGraph()
    val graphInfo = Map("Graph order" -> graph.order.toString,
                        "Graph type" -> graph.graphType,
                        "Graph mean degree" -> graph.meanDegree.toString)

    var flag = false

    (0 until repeatition) foreach { i =>
      println(s"Starting round $i")
      val system = ActorSystem("Gossip" + i)
      val members = graph.nodes map { n =>
        val id = n.id
        id -> system.actorOf(
          Props(new PushPullGossiper(s"node$id", SingleMeanGossiper(data(id)))),
          name = id.toString
        )
      } toMap

      graph.nodes foreach { node =>
        members(node.id) ! InitMessage(node.links map (n => n.name -> members(n.id)) toMap)
      }

      flag = false
      members.values.foreach { m => m ! StartMessage }
      while (!flag) {
        Thread.sleep(200)

        val futures = members.values.toList map { m =>
          (m ? CheckState).mapTo[NodeState]
        }
        val futureList = Future.sequence(futures)
        futureList map { results =>
          flag = results.forall(_.status == GossiperStatus.COMPLETE)
          if (verbose) {
            results.foreach(r => println(r.nodeName + ": " + abs(r.estimate / dataMean - 1)))
          }
        }

        if (flag) { 
          futureList map { nodeStates =>
            val rawReport = ResultAnalyser(dataMean, graph.order, nodeStates).analyse()
            val report = graphInfo ++ rawReport ++ Map("simCounter" -> i.toString)
            println(report)
            ReportGenerator(s"${numNodes}_sim_out.csv").record(report)
          }
          system.terminate
        }
      }
    }
  }

  def batchSim() {
    val repeatedTimes = 2
    val numNodes = 200
    val dataReader = new DataReader() 
    val data = dataReader.read(s"normal_1000_$numNodes.csv.gz")
    sim(numNodes, data, repeatedTimes)    
  }
}
