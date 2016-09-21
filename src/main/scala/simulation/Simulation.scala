package simulation

import actor.PushPullGossiper
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import gossiper._
import graph.GraphFileReader
import message._
import breeze.linalg.DenseVector
import breeze.stats._

import scala.collection.immutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs
import util.Recorder
import scalaz.Scalaz._


object Simulation {

  def sim(numNodes: Int, 
          data: DenseVector[Double], 
          repeatition: Int = 1,
          verbose: Boolean = false) {

    implicit val timeout = Timeout(1 seconds)

    val dataMean = mean(data)
    val graph = GraphFileReader("sf_200_10_0.data.gz").readGraph()
    val graphInfo = Map("Graph order" -> graph.order.toString,
                        "Graph type" -> graph.graphType,
                        "Graph mean degree" -> graph.meanDegree.toString)

    var flag = true
    (0 until repeatition) foreach { i =>
      val system = ActorSystem("Gossip")
      val members = new ArrayBuffer[ActorRef]
      (0 until numNodes) foreach { i =>
        members.append(
          system.actorOf(
            Props(new PushPullGossiper(s"node$i", SingleMeanGossiper(data(i)))),
            name = "" + i
          )
        )
      }

      (0 until numNodes) foreach { m => 
        val node = graph.nodes(m)
        members(m) ! InitMessage(node.links map (n => n.name -> members(n.id)) toMap)
      }

      flag = true
      members.foreach { m => m ! StartMessage }
      while (!flag) {
        Thread.sleep(150)

        val futures = members map { m =>
          (m ? CheckState).mapTo[NodeState]
        }
        val futureList = Future.sequence(futures)
        futureList map { x =>
          flag = x.forall(_.status == GossiperStatus.COMPLETE)
        }

        futureList map { x =>
          for ((m, i) <- members.zipWithIndex) {
            if (verbose) 
              println(m.path.name + " " + abs(x(i).estimate / dataMean - 1))
          }
        }

        if (flag) { 
          futureList map { nodeStates =>
            val output = Recorder.gatherResults(dataMean, graph.order, nodeStates)
            Recorder.record(s"${numNodes}_sim_out", graphInfo |+| output)                    
          }
          system.terminate
        }
      }
    }

  }




}
