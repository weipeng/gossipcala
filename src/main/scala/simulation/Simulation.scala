package simulation

import actor.Monitor
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import com.typesafe.scalalogging.LazyLogging
import gossiper._
import graph.{Graph, GraphFileReader}
import message._
import org.scalatest.Assertions._
import util.Config.simulation
import util.DataReader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


object Simulation extends LazyLogging {

  def simWithRepetition(repeatTimes: Int,
                        data: DenseVector[Double],
                        dataFileName: String,
                        graph: Graph,
                        gossipType: GossipType.Value): Future[Any] = {
    assert(data.size == graph.order)
    implicit val timeout = Timeout(repeatTimes * 10 minutes)
    val name = s"Gossip-$dataFileName"
    val system = ActorSystem(name)
    val monitor = system.actorOf(Monitor.props(data, graph, gossipType, repeatTimes, dataFileName))
    (monitor ? StartMonitor) flatMap {
      case MonitorCompleted => system.terminate()
      case MonitorFailed => Future.failed(new RuntimeException("unexpected error happens"))
      case t: Throwable => Future.failed(t)
    }
  }

  def batchSim(): Unit = {
    val repeatedTimes = 3
    val numNodes = simulation.numNodes
    val dataReader = new DataReader()
    val dataFileName = simulation.dataSource
    val data = dataReader.read(s"${dataFileName}_$numNodes.csv.gz")

    val gt = simulation.gossipType
    val params = for {
      param <- 35 to 35 by 5
      graphIndex <- 0 until 5
    } yield (param, graphIndex)

    val f = params.foldLeft(Future.successful[Any](Unit)) { (f, param) =>
      f.flatMap { _ =>
        val p = param._1
        val graphIndex = param._2
        val graphName = s"sf_${numNodes}_${p}_$graphIndex.data.gz"
        val graph = GraphFileReader(graphName).readGraph()
        logger.info(s"start $graphName")
        simWithRepetition(repeatedTimes, data, dataFileName, graph, gt)
      }
    }
    Await.ready(f, Duration.Inf)
  }
}
