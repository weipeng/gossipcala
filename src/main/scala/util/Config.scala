package util


import gossiper.GossipType
import com.typesafe.config.ConfigFactory

object Config {
  private val config = ConfigFactory.load()

  object algorithm {
    private lazy val algorithmConfig = config.getConfig("algorithm")
    lazy val stoppingThreshold = algorithmConfig.getInt("stopping-threshold")
    lazy val errorBound = algorithmConfig.getDouble("error-bound")
    lazy val wastedRoundThreshold = algorithmConfig.getDouble("wasted-round-threshold")
  }

  object util {
    private lazy val utilConfig = config.getConfig("util")
    lazy val outputPath = utilConfig.getString("output-path")
    lazy val dataPath = utilConfig.getString("data-path")
  }

  object simulation {
    private lazy val simulationConfig = config.getConfig("simulation")
    lazy val checkStateTimeout = simulationConfig.getInt("check-state-timeout")
    lazy val numNodes = simulationConfig.getInt("num-nodes")
    lazy val dataSource = simulationConfig.getString("data-source")
    lazy val gossipType = simulationConfig.getString("gossip-type").toLowerCase  match {  
      case "pushpull" => GossipType.PUSHPULL
      case "pushsum" => GossipType.PUSHSUM
      case "weighted" => GossipType.WEIGHTED
    }
  }
}

