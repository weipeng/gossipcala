package graph

/**
  * Author: yanyang.wang
  * Date: 13/09/2016
  */
case class Graph(name: String, multigraph: Boolean, directed: Boolean, nodes: List[Node]) {
  def nodesNumber: Int = nodes.size

  override def toString(): String = s"Graph($name, $multigraph, $directed, ${nodes.sortBy(_.id)})"
}

case class Node(id: Int, links: List[Node]) {
  def name: String = Node.prefix + ": " + id
  override def toString(): String = s"Node($id, ${links.map(_.id).sorted})"
}

object Node {
  val prefix = "Node"
}
