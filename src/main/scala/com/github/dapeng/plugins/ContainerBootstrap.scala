package com.github.dapeng.plugins

import java.net.URL
import java.util

import com.github.dapeng.bootstrap.Bootstrap
import org.slf4j.{Logger, LoggerFactory}

import collection.JavaConverters._


/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])
  var lastCompiledTime: Long = 0l


  def bootstrap(appClasspaths: Seq[URL], pluginsLibs: util.List[util.List[URL]]): Unit = {
    try {
      Bootstrap.sbtStartup(this.getClass.getClassLoader,
        appClasspaths.toList.asJava,
        pluginsLibs
      )
    }
    catch {
      case ex: Exception => {
        println(ex.getStackTrace)
        logger.error(ex.getMessage, ex)
      }
    }
  }

}

