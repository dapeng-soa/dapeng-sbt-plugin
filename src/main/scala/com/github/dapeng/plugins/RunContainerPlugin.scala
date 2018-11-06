package com.github.dapeng.plugins

import java.io.{File, FileInputStream}
import java.net.URL
import java.util
import java.util.{Properties}

import com.github.dapeng.api.{Container, Plugin}
import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader
import com.github.dapeng.core._
import com.github.dapeng.impl.plugins.{ApiDocPlugin, SpringAppLoader}
import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.{AutoPlugin, _}
import xsbti.compile.CompileAnalysis

import collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin extends AutoPlugin {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)
  val sourceCodeMap = mutable.HashMap[String,Long]()
  var switch2Reload = false

  def runDapeng(appClasspaths: Seq[URL]): Unit = {
    val threadGroup = new ThreadGroup("dapeng")
    val bootstrapThread = new Thread(threadGroup, () => {
      switch2Reload  = true
      new ContainerBootstrap().bootstrap(appClasspaths)
    })
    bootstrapThread.start()

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

      loadSystemProperties(new File(projectPath + "/dapeng.properties"))

      val dependentClasspaths = (fullClasspath in Compile).value.map(
        _.data.toURI.toURL
      )

      val classpathsWithDapeng = dependentClasspaths.toList

      if (switch2Reload) {
        reloadApplication(classpathsWithDapeng)
      }


      if (!switch2Reload) {
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
    val containerClassLoader = this.getClass.getClassLoader
    val containerFactoryClz = containerClassLoader.loadClass("com.github.dapeng.api.ContainerFactory")
    val getContainerMethod = containerFactoryClz.getMethod("getContainer")
    val container = getContainerMethod.invoke(containerFactoryClz)
    val getAppMethod = container.getClass.getMethod("getApplications")
    val oldApplication:Application = getAppMethod.invoke(container).asInstanceOf[java.util.List[Application]].get(0)
    val unregisterAppMethod = container.getClass.getMethod("unregisterApplication", Class.forName("com.github.dapeng.core.Application"))
    unregisterAppMethod.invoke(container,oldApplication)
    println(s"================ unregisterApp [ $oldApplication ] done ================")

    val applicationLibs: List[URL] = appClasspaths.toList
    val appClassLoader = new ApplicationClassLoader(applicationLibs.toArray, null, containerClassLoader)
    val applicationCLs = new util.ArrayList[ClassLoader]
    applicationCLs.add(appClassLoader)

    val getPluginsMethod = container.getClass.getMethod("getPlugins")
    val plugins:util.List[Plugin]= getPluginsMethod.invoke(container).asInstanceOf[java.util.List[Plugin]]

    val getRegPluMethod = container.getClass.getMethod("registerPlugin", Class.forName("com.github.dapeng.api.Plugin"))

    val size = plugins.size() - 1
    for ( i <- (0 to size).reverse){
      if (plugins.get(i).isInstanceOf[SpringAppLoader]) {
        println("============== Re-registration SpringAppLoader plugin ========================")
        plugins.remove(i)
        val newSpringPlugin = new SpringAppLoader(container.asInstanceOf[Container], applicationCLs)
        getRegPluMethod.invoke(container,newSpringPlugin)
        newSpringPlugin.start()
        println("============== new SpringAppLoader plugin  start done ========================")
      }
    }
    plugins.foreach{ item =>
      if (item.isInstanceOf[ApiDocPlugin]) {
        println("============== restart ApiDocPlugin ==========================")
        item.stop()
        item.start()
        println("============== restart ApiDocPlugin done ==========================")
      }
    }
  }
}

