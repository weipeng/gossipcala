package simulation

import actor.{PushPullGossiper, WeightedGossiper}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import breeze.stats._
import com.typesafe.scalalogging.LazyLogging
import gossiper._
import graph.{Graph, GraphFileReader}
import message._
import org.scalatest.Assertions._
import report.{ReportGenerator, ResultAnalyser}
import util.Config.simulation
import util.DataReader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs


object Simulation extends LazyLogging {

  implicit val timeout = Timeout(1 seconds)

  def sim(data: DenseVector[Double], 
          dataFileName: String,
          graph: Graph,
          gossipType: String,
          repeatition: Int,
          round: Int): Future[Unit] = {
      logger.info(s"Starting round $round")
    val dataMean = mean(data)
    val numNodes = graph.order
    assert(data.size == graph.order)

      val system = ActorSystem("Gossip-" + round)
      val members = graph.nodes map { n =>
        val id = n.id
        id -> system.actorOf(
          Props(
            gossipType.toLowerCase match {
              case "pushpull" => new PushPullGossiper(s"node$id", SingleMeanGossiper(data(id)))
              case "weighted" => new WeightedGossiper(s"node$id", SingleMeanGossiper(data(id)))
              case gt => throw new Exception(s"""Gossip type "${gt.toString}" not supported""")
            }
          ),
          name = id.toString
        )
      } toMap

      graph.nodes foreach { node =>
        members(node.id) ! InitMessage(node.links map (n => n.name -> members(n.id)) toMap)
      }

      members.values.foreach { m => m ! StartMessage }

      checkState(members.values.toList)(r => r.nodeName + ": " + abs(r.estimate / dataMean - 1)).flatMap {results =>
        val simCount = round + repeatition * graph.index
        val report = ResultAnalyser(dataMean, results, simCount, gossipType, graph).analyse()

        logger.trace(report.printString)
        ReportGenerator(s"${numNodes}_sim_out_${dataFileName}.csv").record(List(report))
        system.terminate.map(_ => Unit)
      }
  }

  private def reRun(round: Int, limit: Int, f: Int => Future[Unit]): Future[Unit] = {
    if (round >= limit) Future.successful(Unit) else f(round).flatMap(_ => reRun(round + 1, limit, f))
  }

  private def checkState(nodes: List[ActorRef])(log: NodeState => String): Future[List[NodeState]] = {
    Thread.sleep(simulation.checkStateTimeout)
    val futures = nodes map { m =>
      (m ? CheckState).mapTo[NodeState]
    }
    Future.sequence(futures) flatMap { results =>
      results.foreach{r => logger.trace(log(r))}
      val completed = results.forall(_.status == GossiperStatus.COMPLETE)
      if (completed) Future.successful(results) else checkState(nodes)(log)
    }
  }

  def batchSim() {
    val repeatedTimes = 2
    val numNodes = 200
    val dataReader = new DataReader() 
    val dataFileName = "normal_1000"
    val data = dataReader.read(s"${dataFileName}_$numNodes.csv.gz")
    val gossipTypes = List("pushpull", "weighted")

    gossipTypes.take(1) foreach { gt =>
      val params = for {
        param <- 10 to 50 by 5
        graphIndex <- 0 until 5
      } yield (param, graphIndex)

      val fs = params.toList.map { case (param, graphIndex) =>
        val graphName = s"sf_${numNodes}_${param}_$graphIndex.data.gz"
        val graph = GraphFileReader(graphName).readGraph()
        logger.info(s"start $graphName")
        reRun(0, repeatedTimes, sim(data, dataFileName, graph, gt, repeatedTimes, _))
      }
      Future.sequence(fs).map(_ => Unit)
    }
  }
}
