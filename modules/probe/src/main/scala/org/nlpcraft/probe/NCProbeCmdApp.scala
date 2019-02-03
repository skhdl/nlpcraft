/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe

import java.io.{File, FileFilter}
import java.nio.file.Paths

import language._
import org.nlpcraft._
import org.nlpcraft.probe.mgrs.exit.NCExitManager
import org.apache.commons.lang3.SystemUtils
import org.nlpcraft.probe.dev._

import scala.collection.JavaConverters._
import language.postfixOps

/**
  * 
  */
object NCProbeCmdAppChild extends App {
    val id = sys.props("__NC_PROBE_ID")
    val token = sys.props("__NC_PROBE_TOKEN")
    val uplink = sys.props.getOrElse("__NC_PROBE_UPLINK", null) // Optional.
    val downlink = sys.props.getOrElse("__NC_PROBE_DOWNLINK", null) // Optional.
    val jars = sys.props("__NC_PROBE_JARS")
    
    System.exit(NCProbeRunner.startProbe(NCProbeConfigBuilder.newConfig().
        setId(id).
        setToken(token).
        setUpLink(uplink).
        setDownLink(downlink).
        setJarsFolder(jars).
        build()
    ))
}

/**
  * Command line launcher for the probe.
  */
object NCProbeCmdApp extends App {
    private val SEP = if (SystemUtils.IS_OS_WINDOWS) ";" else ":"
    
    /**
      *
      * @param errMsg Message to show before usage.
      */
    private def usage(errMsg: String): Unit = {
        if (errMsg != null)
            println(errMsg)
        
        println("Usage:")
        println("   -id=xxx")
        println("      Sets mandatory probe ID to 'xxx'.")
        println("      Probe ID is a user defined unique probe identifier.")
        println("      Can also be set via 'NLPCRAFT_PROBE_ID' system property or environment variable.")
        println("      Example: -id=dev-probe1")
        println()
        println("   -token=xxx")
        println("      Sets mandatory probe token to 'xxx'.")
        println("      Can also be set via 'NLPCRAFT_PROBE_TOKEN' system property or environment variable.")
        println("      NOTE: probe token must be kept secure.")
        println("      Example: -token=ASD12-IUW12-YTR98-I2N7H")
        println()
        println("   -uplink=host:port")
        println("      Sets up-link to 'host:port'. If not provided - the default up-link is used.")
        println("      Can also be set via 'NLPCRAFT_PROBE_UPLINK' system property or environment variable.")
        println()
        println("   -downlink=host:port")
        println("      Sets down-link to 'host:port'. If not provided - the default down-link is used.")
        println("      Can also be set via 'NLPCRAFT_PROBE_DOWNLINK' system property or environment variable.")
        println()
        println("   -jars=dir")
        println("      Sets JARs folder to 'dir' directory. If not provided - current directory is used instead.")
        println("      All user defined models should be placed as JAR files into this folder.")
        println("      Can also be set via 'NLPCRAFT_PROBE_JARS' system property or environment variable.")
        println("      JARs folder is periodically scanned for automatic hot redeployment.")
        println("      Example: -jars=/opt/nlpcraft/probe/models")
        println()
        println("   -J-XXX")
        println("      Passes JVM parameter '-XXX' to the child JVM process.")
        println("      Example: -J-ea -J-Xms512m -J-Xmx2G")
        println()
        println("   -help or -?")
        println("      Prints this usage information.")
    }
    
    /**
      *
      * @param path Directory path to scan for JARs.
      * @param cp Current class path.
      * @return
      */
    @throws[NCE]
    private def getJarsClassPath(path: String, cp: String): String = {
        val cpFiles = cp.split(SEP)
        
        val dir = new File(path)
        
        if (!dir.exists())
            throw new NCE(s"Path does not exist: $path")
        if (!dir.isDirectory)
            throw new NCE(s"Path is not a directory: $path")
        
        val jars = dir.listFiles(new FileFilter {
            override def accept(f: File): Boolean =
                f.isFile && f.getName.endsWith(".jar") && !cpFiles.contains(f.getName)
        })
        
        if (jars == null) "" else jars.mkString(SEP)
    }
    
    /**
      *
      * @param args List of arguments.
      * @param argName Argument name to find.
      * @param envName Environment variable and system property name to use.
      * @return
      */
    private def optionalArg(args: Array[String], argName: String, envName: String = null): Option[String] = {
        args.find(_.startsWith(argName + "=")) match {
            case Some(arg) ⇒ Some(arg.drop(argName.length + 1))
            case None ⇒
                if (envName == null)
                    None
                else
                    Option(
                        sys.props.getOrElse(
                            envName,
                            sys.env.getOrElse(
                                envName,
                                null
                            )
                        )
                    )
        }
    }
    
    /**
      * @param args List of arguments.
      * @param argName Argument name to find.
      * @param envName Environment variable and system property name to use.
      * @throws NCE Thrown if argument cannot be found.
      * @return
      */
    @throws[NCE]
    private def mandatoryArg(args: Array[String], argName: String, envName: String = null): String = {
        optionalArg(args, argName, envName) match {
            case Some(arg) ⇒
                if (arg.trim.isEmpty)
                    throw new NCE(s"Missing mandatory parameter: $argName")
                else
                    arg
            case None ⇒ throw new NCE(s"Missing mandatory parameter: $argName")
        }
    }
    
    @throws[NCE]
    private def execute(): Unit = {
        val vmArgs = args.filter(_.startsWith("-J")).map(_.drop(2))
        val appArgs = args.filter(!_.startsWith("-J"))
        
        // Check for unknown params.
        for (arg ← args) {
            if (!arg.startsWith("-id=") &&
                !arg.startsWith("-token=") &&
                !arg.startsWith("-uplink=") &&
                !arg.startsWith("-downlink=") &&
                !arg.startsWith("-jars=") &&
                !arg.startsWith("-help") &&
                !arg.startsWith("-?") &&
                !arg.startsWith("-J-="))
                throw new NCE(s"Unknown command line argument: $arg")
        }
        
        if (appArgs.contains("-help") || appArgs.contains("-?"))
            throw new NCE(null)
        
        val curDir = Paths.get(".").toAbsolutePath.normalize().toString
        
        val id = mandatoryArg(appArgs, "-id", "NLPCRAFT_PROBE_ID")
        val token = mandatoryArg(appArgs, "-token", "NLPCRAFT_PROBE_TOKEN")
        val upLink = optionalArg(appArgs, "-uplink", "NLPCRAFT_PROBE_UPLINK")
        val downLink = optionalArg(appArgs, "-downlink", "NLPCRAFT_PROBE_DOWNLINK")
        val jarsDir = optionalArg(appArgs, "-jars", "NLPCRAFT_PROBE_JARS") getOrElse curDir
        
        // Verify the configuration is valid.
        try
            NCProbeConfigBuilder.newConfig().
                setId(id).
                setToken(token).
                setUpLink(upLink orNull).
                setDownLink(downLink orNull).
                setJarsFolder(jarsDir).
                build()
        catch {
            case e: IllegalArgumentException ⇒ throw new NCE(e.getMessage)
        }
    
        var cp = sys.props("java.class.path")
        val cpJars = getJarsClassPath(jarsDir, cp)
    
        if (cpJars.nonEmpty)
            cp = s"$cp$SEP$cpJars"
    
        var params =
            Seq("java") ++
            vmArgs ++
            Seq(s"-D__NC_PROBE_ID=$id") ++
            Seq(s"-D__NC_PROBE_TOKEN=$token") ++
            Seq(s"-D__NC_PROBE_JARS=$jarsDir")
        
        if (upLink.isDefined)
            params = params ++ Seq(s"-D__NC_PROBE_UPLINK=${upLink.get}")
        if (downLink.isDefined)
            params = params ++ Seq(s"-D__NC_PROBE_DOWNLINK=${downLink.get}")
        
        params = params ++ Seq("-classpath", cp, NCProbeCmdAppChild.getClass.getName.takeWhile(_ != '$'))
    
        val procBldr = new ProcessBuilder(params.asJava)
    
        procBldr.directory(new File(curDir))
        procBldr.inheritIO()
    
        var proc: Process = null
    
        def destroyProcess(): Unit =
            procBldr.synchronized {
                G.destroyProcess(proc)
            }
    
        val hook = new Thread("probe-cmd-shutdown-hook") {
            override def run(): Unit = {
                destroyProcess()
            }
        }
    
        Runtime.getRuntime.addShutdownHook(hook)
    
        var code = NCExitManager.RESTART
    
        try
            while (code == NCExitManager.RESTART) {
                procBldr.synchronized {
                    proc = procBldr.start()
                }
            
                code = proc.waitFor()
            }
        catch {
            case e: Exception ⇒
                destroyProcess()
            
                throw e
        }
    
        Runtime.getRuntime.removeShutdownHook(hook)
    }
    
    try
        execute()
    catch {
        case e: NCE ⇒ usage(e.getMessage)
        case e: Throwable ⇒ usage(s"System error: ${e.getMessage}")
    }
}
