package util

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}

object Configs {
  val configPath = "../../config/"
  val filename = configPath + "configurations"
  val config = ConfigFactory.parseFile(new File(filename))
}

