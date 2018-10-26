package com.github.dapeng.plugins

import java.io.{File, FileInputStream}
import java.lang.reflect.{Constructor, Method}
import java.net.URL
import java.util
import java.util.{Optional, Properties}

import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader
import com.github.dapeng.core._
import com.github.dapeng.core.definition.SoaServiceDefinition
import com.github.dapeng.core.helper.SoaSystemEnvProperties
import com.github.dapeng.impl.container.DapengApplication
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

    val applicationLibs: List[URL] = appClasspaths.toList
    val appClassLoader = new ApplicationClassLoader(applicationLibs.toArray, null, containerClassLoader)

    val appCtxClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext")

    val parameterTypes =  classOf[Array[String]]

    val constructor = appCtxClass.getConstructor(parameterTypes)
    val springCtx = getSpringContext(configPath, appClassLoader, constructor)
    val method = appCtxClass.getMethod("getBeansOfType", classOf[Class[_]])
    val processorMap: mutable.Map[String,SoaServiceDefinition[_]] = method.invoke(springCtx, appClassLoader.loadClass(classOf[SoaServiceDefinition[_]].getName))
                                                                  .asInstanceOf[mutable.Map[String, SoaServiceDefinition[_]]]

    val appInfos: mutable.Map[String, ServiceInfo] = toServiceInfos(processorMap)
    val newApplication = new DapengApplication(new util.ArrayList[ServiceInfo](appInfos.values), appClassLoader)
    val registerAppMethod = container.getClass.getMethod("registerApplication", Class.forName("com.github.dapeng.core.Application"))
    registerAppMethod.invoke(container,newApplication)
  }

  def getSpringContext(configPath: String, appClassLoader: ClassLoader, constructor:Constructor[_] ) = {
    val xmlPaths = new mutable.ListBuffer[String]()

    val resources = appClassLoader.getResources(configPath)

    while ( resources.hasMoreElements) {
      val nextElement = resources.nextElement
      if (!nextElement.getFile.matches(".*dapeng-transaction-impl.*"))
        xmlPaths.append(nextElement.toString)
    }
    constructor.newInstance(Array(xmlPaths.head))
  }

  def toServiceInfos(processorMap: mutable.Map[String,SoaServiceDefinition[_]]): mutable.Map[String, ServiceInfo] = {
    val serviceInfoMap = new mutable.HashMap[String, ServiceInfo]
    processorMap.map(pro=>{
      val processorKey = pro._1
      val processor = pro._2
      val count = processor.iface.getClass.getInterfaces.count( clazz => {
        clazz.getClass.getName.equals("org.springframework.aop.framework.Advised")
      })

      val ifaceClass = (if (count > 0) processor.iface.getClass.getMethod("getTargetClass").invoke(processor.iface)
      else processor.iface.getClass).asInstanceOf[Class[_]]
      val service = processor.ifaceClass.getAnnotation(classOf[Service])

      val methodsConfigMap = new mutable.HashMap[String, Optional[CustomConfigInfo]]
      for ((key, function) <- processor.functions) {
        methodsConfigMap.put(key, function.getCustomConfigInfo)
      }

      val serviceVersionAnnotation = if (ifaceClass.isAnnotationPresent(classOf[ServiceVersion])) ifaceClass.getAnnotationsByType(classOf[ServiceVersion])(0)
      else null
      val version = if (serviceVersionAnnotation != null) serviceVersionAnnotation.version
      else service.version

      val methodsMaxProcessTimeMap = new util.HashMap[String, java.lang.Long](16)

      util.Arrays.asList(ifaceClass.getMethods).forEach(item =>
        if (processor.functions.keySet.contains(item.getClass.getName)) {
          var maxProcessTime = SoaSystemEnvProperties.SOA_MAX_PROCESS_TIME
          if (item.getClass.isAnnotationPresent(classOf[MaxProcessTime])) {
            maxProcessTime = item.getClass.getAnnotation(classOf[MaxProcessTime]).maxTime
          }
          methodsMaxProcessTimeMap.put(item.getClass.getName, maxProcessTime)
        })

      if (serviceVersionAnnotation == null || serviceVersionAnnotation.isRegister) {
        val serviceInfo = new ServiceInfo(service.name, version, "service", ifaceClass, processor.getConfigInfo, methodsConfigMap, methodsMaxProcessTimeMap)
        serviceInfoMap.put(processorKey, serviceInfo)
      }
    })
    serviceInfoMap
  }
}

