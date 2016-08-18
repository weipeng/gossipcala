import akka.actor.{Actor,ActorSystem,Props,ActorRef}
import akka.pattern.ask 
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.Map
import scala.language.postfixOps
import gossiper._
import message._
import scala.math.abs


object simulation {
    def main(args: Array[String]) ={
        sim()
    }

    def sim() {
        implicit val timeout = Timeout(1 seconds)

        val system = ActorSystem("Gossip")

        val numNodes = 3
        val members = new ArrayBuffer[ActorRef]
        val data = Array[Double](233, 21, 53)
        val dataSum = data.sum
        for (i <- 0 to numNodes - 1) {
            members.append(
                system.actorOf(Props(new PushPullGossiper(s"node$i", data(i))), name="node"+i) 
            )
        }
    
        members(0) ! InitMessage(Map("node1" -> members(1), 
                                     "node2" -> members(2)))
        members(1) ! InitMessage(Map("node0" -> members(0)))
        members(2) ! InitMessage(Map("node1" -> members(0)))

        members.foreach {m => m ! StartMessage}
    
        var flag = false
        while (!flag) { 
            Thread.sleep(200)

            val futures = members map { m =>
                (m ? AskStateAndEstimateMessage).mapTo[Tuple2[Boolean, Double]]
            }
            val futureList = Future.sequence(futures) 
            futureList map { x =>
                flag = x.foldLeft(true)((a, b) => a && b._1)
            }
            
            futureList map {x => println(x + "AAAAAAAAAAAAAAAAAAH " + flag)} 
            futureList map { x =>
                for ((m, i) <- members.zipWithIndex) {
                    println(m.path.name + " " + abs(x(i)._2 / (dataSum / 3) - 1))
                }
            }
            println("Average " + dataSum/3)

            if (flag) 
                system.terminate
            
        }   
    }
}
