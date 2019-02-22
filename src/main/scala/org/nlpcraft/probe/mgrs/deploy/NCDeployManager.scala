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

package org.nlpcraft.probe.mgrs.deploy

import java.io._
import java.util.jar.{JarInputStream ⇒ JIS}

import org.nlpcraft.common._
import org.nlpcraft.common.{NCDebug, NCLifecycle}
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.model._
import org.nlpcraft.probe.mgrs.NCProbeLifecycle
import resource.managed

import scala.collection.convert.DecorateAsScala
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Exception._

/**
  * Model deployment manager.
  */
object NCDeployManager extends NCProbeLifecycle("Deploy manager") with NCDebug with DecorateAsScala {
    private val providers = ArrayBuffer.empty[NCModelProvider]
    private val descriptors = ArrayBuffer.empty[NCModelDescriptor]
    
    /**
      * Gives a list of JAR files at given path.
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
      * @param clsName Provider class name.
      */
    @throws[NCE]
    private def makeProvider(clsName: String): NCModelProvider =
        try {
            makeProvider(Thread.currentThread().getContextClassLoader.loadClass(clsName), clsName)
        }
        catch {
            case e: Throwable  ⇒
                throw new NCE(s"Failed to load model provider class: $clsName", e)
        }

    /**
      * 
      * @param cls Provider class.
      * @param src Provider class source.
      */
    @throws[NCE]
    private def makeProvider(cls: Class[_], src: String): NCModelProvider =
        catching(classOf[Throwable]) either cls.newInstance().asInstanceOf[NCModelProvider] match {
            case Left(e) ⇒
                throw new NCE(s"Model provider failed to instantiate [" +
                    s"class=${cls.getName}, " +
                    s"source=$src" +
                "]", e)

            case Right(provider) ⇒ provider
        }
    
    /**
      * 
      * @param jarFile JAR file to extract from.
      */
    @throws[NCE]
    private def extractProviders(jarFile: File): Seq[NCModelProvider] = {
        val clsLdr = Thread.currentThread().getContextClassLoader
        
        val classes = mutable.ArrayBuffer.empty[Class[_]]

        managed(new JIS(new BufferedInputStream(new FileInputStream(jarFile)))) acquireAndGet { in ⇒
            var entry = in.getNextJarEntry

            while (entry != null) {
                if (!entry.isDirectory && entry.getName.endsWith(".class")) {
                    val clsName = entry.getName.substring(0, entry.getName.length - 6).replace('/', '.')

                    try {
                        val cls = clsLdr.loadClass(clsName)

                        if (classOf[NCModelProvider].isAssignableFrom(cls) && !cls.isInterface)
                            classes += cls
                    }
                    catch {
                        // Errors are possible for JARs like log4j etc, which have runtime dependencies.
                        // We don't need these messages in log beside trace, so ignore...
                        case _: ClassNotFoundException  ⇒ ()
                        case _: NoClassDefFoundError ⇒ ()
                    }
                }

                entry = in.getNextJarEntry
            }
        }
    
        classes.map(makeProvider(_, jarFile.getPath))
    }
    
    /**
      * Starts this component.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        // Add model provider classes first.
        providers ++= config.modelProviders.map(makeProvider)
        
        if (config.jarsFolder != null) {
            val jarsFile = new File(config.jarsFolder)
            
            if (!jarsFile.exists())
                throw new NCE(s"JAR folder path '${config.jarsFolder}' does not exist.")
            if (!jarsFile.isDirectory)
                throw new NCE(s"JAR folder path '${config.jarsFolder}' is not a directory.")

            val src = this.getClass.getProtectionDomain.getCodeSource
            val locJar = if (src == null) null else new File(src.getLocation.getPath)

            for (jar ← scanJars(jarsFile) if jar != locJar)
                providers ++= extractProviders(jar)
        }
        
        for (x ← providers) {
            val dss = x.getDescriptors.asScala
            
            if (dss.isEmpty)
                throw new NCE(s"Model provider returns no descriptors: ${x.getClass}")
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
        
        if (U.containsDups(descriptors.map(_.getId).toList))
            throw new NCE("Duplicate model IDs detected.")
        
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
