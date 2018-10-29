package com.github.dapeng.plugins

import java.io.{File, FileInputStream}
import java.lang.reflect.{Constructor, Method}
import java.net.URL
import java.util
import java.util.{Optional, Properties}

import com.github.dapeng.api.Plugin
import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader
import com.github.dapeng.core._
import com.github.dapeng.core.definition.SoaServiceDefinition
import com.github.dapeng.core.helper.SoaSystemEnvProperties
import com.github.dapeng.impl.container.DapengApplication
import com.github.dapeng.impl.plugins.{ApiDocPlugin, SpringAppLoader, ZookeeperRegistryPlugin}
import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.{AutoPlugin, _}
import xsbti.compile.CompileAnalysis

import collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin extends AutoPlugin {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)
  val sourceCodeMap = mutable.HashMap[String,Long]()
  var flag = false

  def runDapeng(appClasspaths: Seq[URL]): Unit = {
    val threadGroup = new ThreadGroup("dapeng")
    val bootstrapThread = new Thread(threadGroup, () => {
      flag = true
      new ContainerBootstrap().bootstrap(appClasspaths)
    })
    bootstrapThread.start()

   // bootstrapThread.join()
    flag = false
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

      val compileResule: CompileAnalysis = (compile in Compile).value
      import scala.collection.JavaConverters._
      val changeFile = compileResule.readStamps().getAllProductStamps.asScala.toList

      if (changeFile.nonEmpty && flag) {
        reloadApplication(classpathsWithDapeng)
      }


      if (!flag) {
        runDapeng(classpathsWithDapeng)
      }


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


  def reloadApplication(appClasspaths: Seq[URL]): Unit = {
    println("================ reloadApplication ================")
    val configPath = "META-INF/spring/services.xml"
    val containerClassLoader = this.getClass.getClassLoader
    val containerFactoryClz = containerClassLoader.loadClass("com.github.dapeng.api.ContainerFactory")
    val getContainerMethod = containerFactoryClz.getMethod("getContainer")
    val container = getContainerMethod.invoke(containerFactoryClz)
    val getAppMethod = container.getClass.getMethod("getApplications")
    val oldApplication:Application = getAppMethod.invoke(container).asInstanceOf[java.util.List[Application]].get(0)
    val unregisterAppMethod = container.getClass.getMethod("unregisterApplication", Class.forName("com.github.dapeng.core.Application"))
    unregisterAppMethod.invoke(container,oldApplication)
    println("================ unregisterApp [" + oldApplication + "] done ================")
    val getPluginsMethod = container.getClass.getMethod("getPlugins")
    val plugins:util.List[Plugin]= getPluginsMethod.invoke(container).asInstanceOf[java.util.List[Plugin]]
    plugins.foreach(item => {
      if (item.isInstanceOf[SpringAppLoader] || item.isInstanceOf[ApiDocPlugin]) {
        item.stop()
        item.start()
      }
    }
    )
  }
}

