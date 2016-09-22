package graph

import com.typesafe.scalalogging.LazyLogging
import net.liftweb.json._
import util.GzFileIterator


/**
  * Author: yanyang.wang
  * Date: 13/09/2016
  */
case class GraphFileReader(fileName: String) extends LazyLogging {
  lazy val pathPrefix = "graphs"

  def readGraph(): Graph = parseJson(read())

  private def parseJson(json: String): Graph = {
    logger.info("parsing json data")
    implicit val formats = DefaultFormats
    parse(json).extractOpt[JsonGraph] match {
      case None => throw new RuntimeException("json string is not valid")
      case Some(g) => g.toGraph()
    }
  }

  private def read(): String = {
    val projectPath = new java.io.File(".").getCanonicalPath
    val targetFilePath = List(projectPath, pathPrefix, fileName).mkString("/")
    logger.info(s"reading from $targetFilePath")
    GzFileIterator(targetFilePath).toList match {
      case Nil => throw new RuntimeException(s"$targetFilePath is empty")
      case head :: _ => head
    }
  }
}

case class JsonNode(id: Int)
case class JsonGraphProperty(name: String)
case class JsonLink(source: Int, target: Int)
case class JsonGraph(graph: JsonGraphProperty,
                     nodes: List[JsonNode],
                     links: List[JsonLink],
                     directed: Boolean,
                     multigraph: Boolean,
                     order: Int,
                     index: Int) {
  def toGraph(): Graph = {
    val SFPattern = ".*barabasi_albert_graph.*".r
    val SWPattern = ".*watts_strogatz_graph.*".r 

    val nodeMap = nodes.map(n => n.id -> Node(n.id, List.empty)).toMap

    val linkedNodeMap = links.foldLeft(nodeMap) { (map, link) =>
      val updatedNodes = (map.get(link.source), map.get(link.target)) match {
        case (Some(sourceNode), Some(targetNode)) =>
          List(
            link.source -> sourceNode.copy(links = sourceNode.links :+ targetNode),
            link.target -> targetNode.copy(links = targetNode.links :+ sourceNode)
          )
        case _ => Nil
      }
      map ++ updatedNodes
    }

    val graphType = graph.name match {
      case SFPattern() => "SF"
      case SWPattern() => "SW"
      case _ => ""
    }

    Graph(graph.name, graphType, multigraph, directed, linkedNodeMap.values.toList, order, index)
  }
}
