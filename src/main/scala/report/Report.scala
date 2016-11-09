package report

import gossiper.GossipType
import shapeless._
import shapeless.record._
import shapeless.ops.record._
import shapeless.syntax.singleton._
/**
  * Author: yanyang.wang
  * Date: 23/09/2016
  */
case class Report(graphOrder: Int,
                  graphType: String,
                  graphMeanDegree: Double,
                  graphIndex: Int,
                  simCounter: Int,
                  gossipType: GossipType.Value,
                  meanL1AbsoluteError: Double,
                  varL1AbsoluteError: Double,
                  meanL2Error: Double,
                  varL2Error: Double,
                  meanRounds: Double,
                  varRounds: Double,
                  meanEffectiveRounds: Double,
                  varEffectiveRounds: Double,
                  meanWastedRounds: Double,
                  varWastedRounds: Double,
                  meanMessages: Double,
                  varMessages: Double,
                  meanBusyMessages: Double,
                  varBusyMessages: Double,
                  meanSharedNeighbors: Double) {
  def printString: String = Report.headers.zip(Report.values(this)).map(p => "\t" + p._1 + " => " + p._2).mkString("\n")
}

object Report {
  val lgr = LabelledGeneric[Report]

  def createRecord(report: Report): lgr.Repr = lgr.to(report)

  def headers: List[String] = Keys[lgr.Repr].apply.toList.map(_.toString.replaceOnce("'", ""))

  def values(report: Report): List[String] = createRecord(report).values.toList.map(_.toString)
}
