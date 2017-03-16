package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import breeze.linalg.DenseVector
import breeze.stats._
import gossiper.{GossipType, GossiperStatus, SingleMeanGossiper}
import graph.Graph
import message.{InitMonitor, _}
import report.{ReportGenerator, ResultAnalyser}
import util.Config.simulation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs
import scala.util.{Failure, Success, Try}

class Monitor(data: DenseVector[Double],
              graph: Graph,
              gossipType: GossipType.Value,
              repeatTimes: Int = 1,
              dataFileName: String) extends Actor with ActorLogging {
  val checkStateTimeout = simulation.checkStateTimeout millis
  val dataMean = mean(data)
  var nodeStates: Map[String, NodeState] = Map.empty
  var requester: ActorRef = Actor.noSender

  override def receive: Receive = {
    case StartMonitor =>
      requester = sender()
      context become init(1)
      self ! InitMonitor
  }

  def init(currentRound: Int): Receive = {
    case InitMonitor =>
      log.info(s"Starting round $currentRound")
      Try {
        graph.nodes map { n =>
          val id = n.id
          val nodeName = s"round$currentRound-node$id"
          id -> context.system.actorOf(
            gossipType match {
              case GossipType.PUSHPULL =>
                PushPullGossiper.props(nodeName, SingleMeanGossiper(nodeName, data(id)))
              case GossipType.WEIGHTED =>
                WeightedGossiper.props(nodeName, SingleMeanGossiper(nodeName, data(id)))
              case GossipType.PUSHSUM =>
                PushSumGossiper.props(nodeName, SingleMeanGossiper(nodeName, data(id)))
              case gt =>
                throw new IllegalArgumentException(s"""Gossip type "${gt.toString}" not supported""")
            },
            name = nodeName
          )
        } toMap
      } match {
        case Success(members) =>
          graph.nodes foreach { node =>
            members(node.id) ! InitMessage(node.links map (n => n.name -> members(n.id)) toMap)
          }
          members.values.foreach { m => m ! StartMessage(None) }
          val scheduleCheck = sendRequests(members.values.toList, 0)
          context become checkState(currentRound, 0, members.values.toList, scheduleCheck)

        case Failure(t) =>
          log.error(t.getMessage)
          requester ! MonitorFailed
          context stop self
      }
  }

  def checkState(currentRound: Int, checkRound: Int, children: List[ActorRef], scheduleCheck: Cancellable): Receive = {
    case state: NodeState if state.checkRound >= checkRound =>
      nodeStates += (state.nodeName -> state)
      if (nodeStates.keys.size == children.size) {
        nodeStates foreach { case (_, r) =>
          log.debug(r.nodeName + ": " + abs(r.estimate / dataMean - 1))
          log.debug(r.toString)
        }
        if (allNodeCompleted) {
          writeReport(currentRound)
          scheduleCheck.cancel()
          children.foreach(context stop)
          if (currentRound >= repeatTimes) {
            requester ! MonitorCompleted
            context stop self
          } else {
            nodeStates = Map.empty
            context become init(currentRound + 1)
            self ! InitMonitor
          }
        }
      }
  }

  private def sendRequests(targets: List[ActorRef], checkRound: Int): Cancellable = {
    context.system.scheduler.schedule(
      checkStateTimeout,
      checkStateTimeout,
      new Runnable {
        override def run(): Unit = targets.foreach(_ ! CheckState(checkRound))
      }
    )
  }

  private def allNodeCompleted: Boolean = nodeStates.values.forall(_.status == GossiperStatus.COMPLETE)

  private def writeReport(currentRound: Int) = {
    val simCounter = currentRound + repeatTimes * graph.index
    val results = nodeStates.values.toList
    val report = ResultAnalyser(dataMean, results, simCounter, gossipType, graph).analyse()

    log.info("dataMean => " + dataMean)
    log.info("mean estimate =>" + mean(results.map(_.estimate)))
    log.info(report.printString)
    ReportGenerator(s"${graph.order}_sim_out_${dataFileName}_${gossipType.toString}.csv").record(List(report))
  }
}

object Monitor {
  def props(data: DenseVector[Double],
            graph: Graph,
            gossipType: GossipType.Value,
            repeatTimes: Int,
            dataFileName: String) = Props(new Monitor(data, graph, gossipType, repeatTimes, dataFileName))
}

