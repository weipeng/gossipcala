package util

import java.io.{File, BufferedReader, InputStreamReader, FileInputStream}
import java.util.zip.GZIPInputStream

class BufferedReaderIterator(reader: BufferedReader) extends Iterator[String] {
  override def hasNext() = reader.ready
  override def next() = reader.readLine()
}

object GzFileIterator {
  val UTF8 = "UTF-8"
  def apply(filePath: String, encoding: String = UTF8) = {
    new BufferedReaderIterator(
      new BufferedReader(
        new InputStreamReader(
          new GZIPInputStream(
            new FileInputStream(new File(filePath))), encoding)))
  }
}
