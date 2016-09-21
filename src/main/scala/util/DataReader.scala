package util

import com.typesafe.scalalogging.LazyLogging
import breeze.linalg.DenseVector


class DataReader extends LazyLogging {

  def read(fileName: String): DenseVector[Double] = {
    val pathPrefix = Config.util.dataPath
    val projectPath = new java.io.File(".").getCanonicalPath
    val targetFilePath = List(projectPath, pathPrefix, fileName).mkString("/")

    logger.info(s"reading synthetic data from $targetFilePath")
    val data = GzFileIterator(targetFilePath).toList.map{_.toDouble} 
    data match {
      case Nil =>
        throw new RuntimeException(s"$targetFilePath is empty")
      case x :: _ => x
    }
    DenseVector(data.toArray)
  }
}
