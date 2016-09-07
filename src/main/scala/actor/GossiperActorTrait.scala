package actor

import akka.actor.{ActorRef, Actor}
import gossiper.{SingleMeanGossiper, AggregateGossiper}

import scala.collection.immutable.Map

/**
  * Author: yanyang.wang
  * Date: 07/09/2016
  */
trait GossiperActorTrait[T, A <: AggregateGossiper[T]] extends Actor {
  val name: String
  protected val gossiper: A
  def gossip(neighbors: Map[String, ActorRef], gossiper: A): A

  override def receive: Receive = work(Map.empty, gossiper)
  def work(neighbors: Map[String, ActorRef], gossiper: A): Receive
}
