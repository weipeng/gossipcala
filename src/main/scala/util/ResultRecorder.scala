package util

import com.github.tototoshi.csv.CSVWriter
import java.io.File
import util.Config.config
import scala.collection.immutable.Map
import scala.collection.mutable.ArrayBuffer
import message.NodeState
import breeze.linalg._
import breeze.numerics._
import breeze.stats._
import graph.Graph


object Recorder {
  def record(fileName: String, data: Map[String, String]) {
    val pathPrefix = Config.util.outputPath
    val projectPath = new File(".").getCanonicalPath
    val targetFilePath = List(projectPath, pathPrefix, fileName+".csv").mkString("/")
    val file = new File(targetFilePath)

    val writer = CSVWriter.open(targetFilePath, append=file.exists)

    if (file.exists) 
      writer.writeRow(data.keys.toSeq)

    writer.writeRow(data.values.toSeq)
    writer.close()
  }

  def gatherResults(dataMean: Double, 
                    graph: Graph, 
                    nodeStates: ArrayBuffer[NodeState]): Map[String, String] = {
    val rounds = DenseVector.zeros[Double](graph.order)
    val messages = DenseVector.zeros[Double](graph.order)
    val wastedRnds = DenseVector.zeros[Double](graph.order)
    val errors = DenseVector.zeros[Double](graph.order)

    for ((n, i) <- nodeStates.zipWithIndex) {
      rounds(i) = n.roundCount
      wastedRnds(i) = n.wastedRoundCount
      messages(i) = n.messageCount
      errors(i) = n.estimate / dataMean - 1
    }
    
    Map(
      "Graph order" -> graph.order.toString,
      "Graph type" -> graph.graphType,
      "Graph mean degree" -> graph.meanDegree.toString,
      "Mean L1-absolute error" -> mean(abs(errors)).toString,
      "Var L1-absolute error" -> variance(abs(errors)).toString,
      "Mean L2 error" -> mean(pow(errors, 2)).toString,
      "Var L2 error" -> variance(pow(errors, 2)).toString,
      "Mean rounds" -> mean(rounds).toString,
      "Var rounds" -> variance(rounds).toString,
      "Mean wasted rounds" -> mean(wastedRnds).toString,
      "Var wasted rounds" -> variance(wastedRnds).toString,
      "Mean messages" -> mean(messages).toString,
      "Var messages" -> variance(messages).toString
    )
  }
}
