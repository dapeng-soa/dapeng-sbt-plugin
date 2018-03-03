package com.github.dapeng.plugins

import java.io.{File, FileInputStream}
import java.net.URL
import java.util.Properties

import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.{AutoPlugin, _}

import collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin extends AutoPlugin {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)
  val sourceCodeMap = mutable.HashMap[String,Long]()

  def runDapeng(appClasspaths: Seq[URL]): Unit = {
    val threadGroup = new ThreadGroup("dapeng")
    val bootstrapThread = new Thread(threadGroup, () => {
      new ContainerBootstrap().bootstrap(appClasspaths)
    })
    bootstrapThread.start()

    bootstrapThread.join()
  }

  def loadSystemProperties(file: File): Unit = {
    if (file.canRead) {
      val properties = new Properties()
      properties.load(new FileInputStream(file))

      val results = properties.keySet().map(_.toString)
      results.foreach(keyString => {
        System.setProperty(keyString, properties.getProperty(keyString))
      })
    }
  }

  override lazy val projectSettings = Seq(
    runContainer := {
      logger.info("starting dapeng container....")
      val projectPath: String = (baseDirectory in Compile).value.getAbsolutePath
      System.setProperty("soa.base", projectPath)

      println(s" projectPath: ${projectPath}")

      //val scalaSourceFiles = getSourceFiles(s"${projectPath}../../../src/main")

      loadSystemProperties(new File(projectPath + "/dapeng.properties"))

      val dependentClasspaths = (fullClasspath in Compile).value.map(
        _.data.toURI.toURL
      )

      val classpathsWithDapeng = dependentClasspaths.toList
      runDapeng(classpathsWithDapeng)
    },

    logLevel in runContainer := Level.Info
  )

  def getSourceFiles(path: String): List[File] = {
    if (!new File(path).isDirectory) {
      List(new File(path))
    } else {
      new File(path).listFiles().flatMap(i => getSourceFiles(i.getPath)).toList
    }
  }

}

