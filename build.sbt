import sbt.Keys._

name := "GossipCala"

version := "1.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalanlp" %% "breeze" % "0.12",
  "org.scalanlp" %% "breeze-viz" % "0.12",
  "com.typesafe.akka" %% "akka-actor" % "2.4.9",
  "com.typesafe.akka" %% "akka-agent" % "2.4.9",
  "net.liftweb" %% "lift-json" % "2.6",
  "com.assembla.scala-incubator" %% "graph-core" % "1.11.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.github.tototoshi" %% "scala-csv" % "1.3.3",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" 
)

val resolvers = Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation"
  //"-Ybackend:o3"
)
