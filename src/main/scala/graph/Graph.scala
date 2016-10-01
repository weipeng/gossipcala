package graph

import breeze.linalg.DenseVector
import breeze.stats._
/**
  * Author: yanyang.wang
  * Date: 13/09/2016
  */
case class Graph(name: String, 
                 graphType: String, 
                 multigraph: Boolean, 
                 directed: Boolean, 
                 nodes: List[Node],
                 order: Int,
                 index: Int,
                 meanSharedNeighbors: Double) {

  def degrees: Array[Double] = nodes.map(_.links.size.toDouble).toArray
  def meanDegree: Double = mean(new DenseVector(degrees))
  def varDegree: Double = variance(new DenseVector(degrees))


  override def toString(): String = s"Graph($name, $multigraph, $directed, ${nodes.sortBy(_.id)})"
}

case class Node(id: Int, links: List[Node]) {
  def name: String = s"${Node.prefix}: $id"
  override def toString(): String = s"${Node.prefix}($id, ${links.map(_.id).sorted})"
}

object Node {
  val prefix = "Node"
}
