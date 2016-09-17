package util

import com.github.tototoshi.csv.CSVWriter
import java.io.File
import util.Configs.config


object Recorder {
  def record(filename: String, data: Map[String, String]) {
    val path = config.getString("outputPath")
    val file = new File(s"$path$filename.csv")
    /*val writer = file.exists match {
      case true => CSVWriter.open(file, append=true)
      case false => CSVWriter.open(file)
    }*/
    val writer = CSVWriter.open(file, append=file.exists)

    if (file.exists) 
      writer.writeRow(data.keys.toSeq)

    writer.writeRow(data.values.toSeq)
    writer.close()
  }
}
