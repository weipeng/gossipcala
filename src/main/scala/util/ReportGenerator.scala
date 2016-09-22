package util

import java.io.{File, IOException}

import com.github.tototoshi.csv.CSVWriter
import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable.Map

case class ReportGenerator(fileName: String) extends LazyLogging {
  def record(data: Map[String, String]): Unit = {
    val pathPrefix = Config.util.outputPath
    val projectPath = new File(".").getCanonicalPath
    val targetFilePath = List(projectPath, pathPrefix, fileName).mkString("/")
    val file = new File(targetFilePath)

    try {
      file.getParentFile.mkdirs()
      val writer = CSVWriter.open(targetFilePath, append = file.exists)

      if (!file.exists) {
        writer.writeRow(data.keys.toSeq)
      }
      writer.writeRow(data.values.toSeq)
      writer.close()
    } catch {
      case ex: IOException => {
        logger.info("IO Exception")
      }
    }
  }
}
