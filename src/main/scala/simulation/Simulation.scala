package simulation

import actor.{PushPullGossiper, WeightedGossiper}
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import breeze.stats._
import gossiper._
import graph.{Graph, GraphFileReader}
import message._
import util.{ResultAnalyser, DataReader, ReportGenerator}
import util.Config.simulation

import scala.collection.immutable.{ListMap, Map}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import org.scalatest.Assertions._
import scala.math.abs


object Simulation {

  def sim(data: DenseVector[Double], 
          graph: Graph,
          gossipType: String,
          repeatition: Int = 1,
          verbose: Boolean = false) {

    implicit val timeout = Timeout(1 seconds)

    val dataMean = mean(data)
    val numNodes = graph.order
    assert(data.size == graph.order)

    val graphInfo = ListMap("Graph Order" -> graph.order.toString,
                            "Graph Type" -> graph.graphType,
                            "Graph Mean Degree" -> graph.meanDegree.toString,
                            "Graph Index" -> graph.index.toString)

    /*val Gossiper = gossipType.toLowerCase match {
      case "pushpull" => PushPullGossiper 
      case "weighted" => WeightedGossiper 
      case _ => throw new Exception("Gossip type not supported")
    }*/

    var flag = false
    (0 until repeatition) foreach { i =>
      if (i % 10 == 0) 
        println(s"Starting round $i")

      val system = ActorSystem("Gossip-" + i)
      val members = graph.nodes map { n =>
        val id = n.id
        id -> system.actorOf(
          Props( 
            gossipType.toLowerCase match {  
              case "pushpull" => new PushPullGossiper(s"node$id", SingleMeanGossiper(data(id)))
              case "weighted" => new WeightedGossiper(s"node$id", SingleMeanGossiper(data(id)))
              case _ => throw new Exception("Gossip type not supported")
            }
          ),
          name = id.toString
        )
      } toMap

      graph.nodes foreach { node =>
        members(node.id) ! InitMessage(node.links map (n => n.name -> members(n.id)) toMap)
      }

      flag = false
      members.values.foreach { m => m ! StartMessage }
      while (!flag) {
        Thread.sleep(simulation.checkStateTimeout)

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
            val report = graphInfo ++ rawReport ++ 
                         ListMap("Sim Counter" -> (i + repeatition * graph.index).toString, 
                                 "Gossip Type" -> gossipType)
            if (verbose) 
              println(report)
            ReportGenerator(s"${numNodes}_sim_out.csv").record(report)
          }
          system.terminate
          //Await.ready(system.whenTerminated, 10 seconds)
        }
      }
    }
  }

  def batchSim() {
    val repeatedTimes = 40
    val numNodes = 200
    val dataReader = new DataReader() 
    val data = dataReader.read(s"normal_1000_$numNodes.csv.gz")
    val gossipTypes = List("pushpull", "weighted")

    gossipTypes.take(1) foreach { gt =>
      for (param <- 10 to 50 by 5) {
        for (graphIndex <- 0 until 5) {
          val graph = GraphFileReader(s"sf_${numNodes}_${param}_${graphIndex}.data.gz").readGraph()
          sim(data, graph, gt, repeatedTimes)    
        }
      }
    }
  }
}
