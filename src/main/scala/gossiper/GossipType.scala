package gossiper

/**
  * Author: yanyang.wang
  * Date: 12/10/2016
  */
object GossipType extends Enumeration {
  val PUSHPULL,
      WEIGHTED,
      PUSHSUM = Value
}

