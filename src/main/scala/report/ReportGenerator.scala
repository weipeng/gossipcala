package report

import java.io.{File, IOException}

import com.github.tototoshi.csv.CSVWriter
import com.typesafe.scalalogging.LazyLogging
import util.Config

case class ReportGenerator(fileName: String) extends LazyLogging {
  def record(data: List[Report]): Unit = {
    val pathPrefix = Config.util.outputPath
    val projectPath = new File(".").getCanonicalPath
    val targetFilePath = List(projectPath, pathPrefix, fileName).mkString("/")
    val file = new File(targetFilePath)
    val ifAppend = file.exists

    try {
      file.getParentFile.mkdirs()
      val writer = CSVWriter.open(targetFilePath, append = ifAppend)
      if (!ifAppend) {
        writer.writeRow(Report.headers)
      }
      data.foreach(r => writer.writeRow(Report.values(r)))
      writer.close()
    } catch {
      case ex: IOException => {
        logger.info("IO Exception")
      }
    }
  }
}
