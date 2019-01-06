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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.probe.mgrs.deploy

import java.io._
import java.nio.file._
import java.util.jar.{JarInputStream ⇒ JIS}
import StandardWatchEventKinds._

import org.nlpcraft.probe.NCProbeManager
import org.nlpcraft.probe.mgrs.exit.{NCExitManager ⇒ ExitMgr}
import org.nlpcraft._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.mdllib._

import scala.collection.convert.DecorateAsScala
import resource.managed

import scala.util.control.Exception._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Model deployment manager.
  */
object NCDeployManager extends NCProbeManager("PROBE deploy manager") with NCDebug with DecorateAsScala {
    private val providers = ArrayBuffer.empty[NCModelProvider]
    private val descriptors = ArrayBuffer.empty[NCModelDescriptor]
    
    private var watcherThread: Thread = _
    
    /**
      * 
      * @param path Path to scan.
      * @return
      */
    private def scanJars(path: File): Seq[File] = {
        val jars = path.listFiles(new FileFilter {
            override def accept(f: File): Boolean =
                f.isFile && f.getName.toLowerCase.endsWith(".jar")
        })

        if (jars == null)
            Seq.empty
        else
            jars.toSeq
    }
    
    /**
      * 
      * @param cls Provider class.
      * @param src Provider class source.
      */
    @throws[NCE]
    private def makeProvider(cls: Class[_], src: String): Option[NCModelProvider] =
        catching(classOf[Throwable]) either cls.newInstance().asInstanceOf[NCModelProvider] match {
            case Left(_) ⇒
                logger.error(s"Failed to instantiate model provider (use 'NCActiveModelProvider' " +
                    s"to mark deployable providers) [" +
                    s"class=${cls.getName}, " +
                    s"source=$src" +
                    s"]"
                )
                
                None

            case Right(provider) ⇒ Some(provider)
        }
    
    /**
      * 
      * @param jarFile JAR file to extract from.
      */
    @throws[NCE]
    private def extractProviders(jarFile: File): Seq[NCModelProvider] = {
        // Ack entry.
        logger.trace(s"Scanning: $jarFile")
        
        val clsLdr = Thread.currentThread().getContextClassLoader
        
        val classes = mutable.ArrayBuffer.empty[Class[_]]

        managed(new JIS(new BufferedInputStream(new FileInputStream(jarFile)))) acquireAndGet { in ⇒
            var entry = in.getNextJarEntry

            while (entry != null) {
                if (!entry.isDirectory && entry.getName.endsWith(".class")) {
                    val clsName = entry.getName.substring(0, entry.getName.length - 6).replace('/', '.')

                    try {
                        val cls = clsLdr.loadClass(clsName)

                        if (classOf[NCModelProvider].isAssignableFrom(cls) && !cls.isInterface) {
                            classes += cls
                            
                            logger.trace(s"Detected model provider [" +
                                s"class=$cls, " +
                                s"jarFile=$jarFile" +
                                s"]")
                        }
                    }
                    catch {
                        // Errors are possible for JARs like log4j etc, which have runtime dependencies.
                        // We don't need these messages in log beside trace.
                        case _: ClassNotFoundException  ⇒
                            logger.trace(s"Model JAR class not found (ignoring) [" +
                                s"jarFile=$jarFile, " +
                                s"class=$clsName" +
                                s"]")
                        case _: NoClassDefFoundError ⇒
                            logger.trace(s"Model JAR no class definition found (ignoring) [" +
                                s"jarFile=$jarFile, " +
                                s"class=$clsName" +
                                s"]")
                    }
                }

                entry = in.getNextJarEntry
            }
        }
        
        // If there is only one class - take it. If more than one - take only those with
        // 'NCActiveModelProvider' annotation.
        val filtered =
            if (classes.size <= 1)
                classes
            else {
                val x = classes.filter(_.isAnnotationPresent(classOf[NCActiveModelProvider]))

                if (x.isEmpty)
                    logger.warn(s"Found multiple model providers - but none have 'NCActiveModelProvider' annotation: $jarFile")

                x
            }
    
        val seq = filtered.flatMap(makeProvider(_, jarFile.getPath))
        
        // Ack exit.
        logger.trace(s"Finished scanning JAR: $jarFile")
        
        seq
    }
    
    /**
      * 
      * @param dir Directory to watch.
      */
    @throws[NCE]
    private def startJarsWatcher(dir: Path): Unit = {
        try {
            val watcher = FileSystems.getDefault.newWatchService()
    
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    
            watcherThread = new Thread() {
                override def run(): Unit = {
                    // Sleep for arbitrary 3 secs. to make sure probe has enough time to start
                    // before we start detecting changes...
                    Thread.sleep(3000)
    
                    logger.trace(s"Watching for changes in: $dir")
                    
                    while (!isInterrupted) {
                        try {
                            // Wait for changes...
                            val key = watcher.take()
                            
                            var jarsChanged = false
                            
                            // Make sure we react to JAR changes only.
                            for (evt ← key.pollEvents.asScala)
                                if (evt.kind() != OVERFLOW &&
                                    evt.asInstanceOf[WatchEvent[Path]].context().toString.endsWith(".jar"))
                                    jarsChanged = true
                            
                            if (!key.reset()) {
                                // In case directory was removed.
                                logger.error(s"Watch service failed for: $dir")
                                
                                interrupt()
                            }
                            
                            if (jarsChanged) {
                                if (ExitMgr.isStarted) {
                                    logger.info(s"New changes detected in '$dir' - restarting probe.")
                                    
                                    ExitMgr.restart()
                                }
                                else
                                    logger.warn(s"JARs folder changes detected but probe hasn't started yet (ignoring).")
                            }
                        }
                        catch {
                            case _: InterruptedException ⇒ interrupt()
                        }
                    }
            
                    watcher.close()
                }
            }
            
            watcherThread.start()
        }
        catch {
            case e: Throwable ⇒ throw new NCE("Failed to start directory watch service.", e)
        }
    }
    
    /**
      * Starts this component.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        require(ExitMgr.isStarted)
        
        if (config.getProvider == null && config.getJarsFolder == null)
            // This is essentially an assertion.
            throw new NCE("Neither provider nor JARs folder are specified.")
        
        val p = config.getProvider
        
        if (p != null)
            providers += p
        
        if (config.getJarsFolder != null) {
            val jarsFile = new File(config.getJarsFolder)
            
            if (!jarsFile.exists())
                throw new NCE(s"JAR folder path '${config.getJarsFolder}' does not exist.")
            if (!jarsFile.isDirectory)
                throw new NCE(s"JAR folder path '${config.getJarsFolder}' is not a directory.")

            val src = this.getClass.getProtectionDomain.getCodeSource
            val locJar = if (src == null) null else new File(src.getLocation.getPath)

            for (jar ← scanJars(jarsFile) if jar != locJar)
                providers ++= extractProviders(jar)
            
            // Start watcher for JARs folder.
            startJarsWatcher(jarsFile.toPath)
        }
        
        if (providers.isEmpty) {
            require(config.getProvider == null)
            
            if (config.getJarsFolder != null) {
                logger.warn("No model providers found on start.")
                logger.info(s"Deploy your model JARs into '${config.getJarsFolder}' folder.")
            }
            else
                logger.error("No model providers found on start and no JAR folder is provided.")
        }
        else {
            for (x ← providers) {
                val dss = x.getDescriptors.asScala
                
                if (dss.isEmpty)
                    logger.error(s"Model provider returns no descriptors: ${x.getClass}")
                else {
                    for (ds ← dss) {
                        val errs = verifyDescriptor(ds)
                        
                        if (errs.nonEmpty) {
                            def nvl(obj: Any): String = if (obj == null) "<null>" else obj.toString
    
                            val tbl = NCAsciiTable()
                            
                            tbl += ("ID", nvl(ds.getId))
                            tbl += ("Name", nvl(ds.getName))
                            tbl += ("Version", nvl(ds.getVersion))
                            tbl += ("Errors", errs.map("- " + _))
                            
                            tbl.error(logger, Some("Model won't deploy due to invalid descriptor:"))
                        }
                        else
                            descriptors += ds
                    }
                }
            }
            
            if (G.containsDups(descriptors.map(_.getId).toList))
                throw new NCE("Duplicate model IDs detected.")
        }
        
        super.start()
    }
    
    /**
      * Verifies given model deployment descriptor.
      *
      * @param ds Descriptor to verify.
      * @return List of error messages. If empty - the verification succeeded.
      */
    private def verifyDescriptor(ds: NCModelDescriptor): List[String] = {
        val errs = mutable.ListBuffer.empty[String]
        
        val mdlName = ds.getName
        val mdlId = ds.getId
        val mdlVer = ds.getVersion
        
        if (mdlId == null)
            errs += s"Model descriptor ID is not provided."
        if (mdlName == null)
            errs += s"Model descriptor name is not provided."
        if (mdlVer == null)
            errs += s"Model descriptor version is not provided."
        
        if (mdlName != null && mdlName.isEmpty)
            errs += s"Model name cannot be empty string."
        if (mdlId != null && mdlId.isEmpty)
            errs += s"Model ID cannot be empty string."
        if (mdlVer != null && mdlVer.length > 16)
            errs += s"Model version cannot be empty string."
        
        if (mdlName != null && mdlName.length > 64)
            errs += s"Model name is too long (64 max): $mdlName"
        if (mdlId != null && mdlId.length > 32)
            errs += s"Model ID is too long (32 max): $mdlId"
        if (mdlVer != null && mdlVer.length > 16)
            errs += s"Model version is too long (16 max): $mdlVer"
        
        errs.toList
    }
    
    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        G.stopThread(watcherThread)
        
        super.stop()
    }
    
    /**
      *
      * @return
      */
    def getDescriptors: Seq[NCModelDescriptor] = {
        ensureStarted()
        
        descriptors
    }

    /**
      *
      * @return
      */
    def getProviders: Seq[NCModelProvider] = {
        ensureStarted()
        
        providers
    }
}
