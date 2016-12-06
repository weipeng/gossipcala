package actor

import scala.collection.immutable.Map
import akka.actor.{Actor, ActorRef}
import gossiper.AggregateGossiper


trait BinaryGossiperTrait[T, A <: AggregateGossiper, E <: ExtraState] extends GossiperActorTrait[T, A, E] {

  protected def nextNeighbor(neighbors: Map[String, ActorRef], banNeighbor: Option[ActorRef]): ActorRef = {
    val eligibleNeighbours = (neighbors.values.toSet -- banNeighbor).toArray[ActorRef]
    (banNeighbor, eligibleNeighbours) match {
      case (Some(n), Array()) => n
      case _ => eligibleNeighbours(rnd.nextInt(eligibleNeighbours.length))
    }
  }
  
}
