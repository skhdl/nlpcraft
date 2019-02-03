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

package org.nlpcraft.util

import java.io._
import java.math.RoundingMode
import java.net.{ServerSocket, Socket, URLDecoder}
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.concurrent.{ExecutorService, TimeUnit}
import java.util.jar.JarFile
import java.util.stream.Collectors
import java.util.zip.{ZipInputStream, GZIPInputStream ⇒ GIS, GZIPOutputStream ⇒ GOS}
import java.util.{Locale, Properties, Random, Timer, TimerTask, UUID, Calendar ⇒ C}

import org.nlpcraft._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import resource._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.language.implicitConversions
import scala.sys.SystemProperties
import scala.util.control.Exception.ignoring
import scala.collection.JavaConverters._
import scala.collection._

/**
  * Project-wide, global utilities ans miscellaneous functions.
  */
object NCGlobals extends NCDebug with LazyLogging {
    // Various decimal formats.
    private final val DEC_FMT0 = mkDecimalFormat("#0")
    private final val DEC_FMT1 = mkDecimalFormat("#0.0")
    private final val DEC_FMT2 = mkDecimalFormat("#0.00")
    
    // Regex for private JSON information hiding.
    private final val JS_VAL = "[ ]?\"[^\"]*\""
    private final val PASSWD_REGEX = s""""passwd":$JS_VAL""".r
    private final val PASSWORD_REGEX = s""""password":$JS_VAL""".r
    private final val OLD_PASSWD_REGEX = s""""oldPassword":$JS_VAL""".r
    private final val NEW_PASSWD_REGEX = s""""newPassword":$JS_VAL""".r
    private final val LOGIN_TKN_REGEX = s""""loginTkn":$JS_VAL""".r
    
    // Regex for private URI information hiding.
    private final val GET_VAL = """[^&,]*"""
    private final val PROBE_TKN_REGEX = s"probeToken=$GET_VAL".r
    private final val ACCESS_TKN_REGEX = s"accessToken=$GET_VAL".r

    private final lazy val DEC_FMT_SYMS = new DecimalFormatSymbols(Locale.US)

    private def mkDecimalFormat(ptrn: String) = {
        val df = new DecimalFormat(ptrn, DEC_FMT_SYMS)
        
        df.setRoundingMode(RoundingMode.DOWN)
        
        df
    }
    
    // OS kinds.
    private var win95 = false
    private var win98 = false
    private var winNt = false
    private var winVista = false
    private var win7 = false
    private var win8 = false
    private var unknownWin = false
    private var win2k = false
    private var winXp = false
    private var win2003 = false
    private var win2008 = false
    private var unix = false
    private var solaris = false
    private var linux = false
    private var netware = false
    private var mac = false
    
    // Unix suffixes.
    private final val UNIX_SFX = Seq("ix", "inux", "olaris", "un", "ux", "sco", "bsd", "att")
    
    detectOs()
    
    // Detects current OS.
    private def detectOs() {
        val os = sys.props("os.name").toLowerCase
        
        if (os.contains("win"))
            if (os.contains("95")) win95 = true
            else if (os.contains("98")) win98 = true
            else if (os.contains("nt")) winNt = true
            else if (os.contains("2000")) win2k = true
            else if (os.contains("vista")) winVista = true
            else if (os.contains("xp")) winXp = true
            else if (os.contains("2003")) win2003 = true
            else if (os.contains("2008")) win2008 = true
            else if (os.contains("7")) win7 = true
            else if (os.contains("8")) win8 = true
            else unknownWin = true
        else if (os.contains("netware")) netware = true
        else if (os.contains("mac os")) mac = true
        else {
            unix = UNIX_SFX.exists(os.contains(_))
            
            if (os.contains("olaris")) solaris = true
            else if (os.contains("inux")) linux = true
        }
    }
    
    // OS kinds.
    lazy val isWindow95: Boolean = win95
    lazy val isWindow98: Boolean = win98
    lazy val isWindowNt: Boolean = winNt
    lazy val isWindowsVista: Boolean = winVista
    lazy val isWindows7: Boolean = win7
    lazy val isWindows8: Boolean = win8
    lazy val isUnknownWindows: Boolean = unknownWin
    lazy val isWindows2k: Boolean = win2k
    lazy val isWindowsXp: Boolean = winXp
    lazy val isWindows2003: Boolean = win2003
    lazy val isWindows2008: Boolean = win2008
    lazy val isUnix: Boolean = unix
    lazy val isSolaris: Boolean = solaris
    lazy val isLinux: Boolean = linux
    lazy val isNetware: Boolean = netware
    lazy val isMac: Boolean = mac
    lazy val isNix: Boolean = mac || linux || solaris || unix
    lazy val isWindows: Boolean = win95 || win98 || winNt || winVista || win7 || win8 || win2k || winXp || win2003 || win2008
    
    private final val UTC = ZoneId.of("UTC")
    
    private final val RND = new Random()
    
    private val sysProps = new SystemProperties
    
    /**
      * Gets now in UTC timezone.
      */
    def nowUtc(): ZonedDateTime = ZonedDateTime.now(UTC)
    
    /**
      * Gets now in UTC timezone in milliseconds representation.
      */
    def nowUtcMs(): Long = Instant.now().toEpochMilli
    
    /**
      * Escapes given string for JSON according to RFC 4627 http://www.ietf.org/rfc/rfc4627.txt.
      *
      * @param s String to escape.
      * @return Escaped string.
      */
    def escapeJson(s: String): String = {
        val len = s.length
        
        if (len == 0)
            ""
        else {
            val sb = new StringBuilder
            
            for (ch ← s.toCharArray)
                ch match {
                    case '\\' | '"' ⇒ sb += '\\' += ch
                    case '/' ⇒ sb += '\\' += ch
                    case '\b' ⇒ sb ++= "\\b"
                    case '\t' ⇒ sb ++= "\\t"
                    case '\n' ⇒ sb ++= "\\n"
                    case '\f' ⇒ sb ++= "\\f"
                    case '\r' ⇒ sb ++= "\\r"
                    case _ ⇒
                        if (ch < ' ') {
                            val t = "000" + Integer.toHexString(ch)
                            
                            sb ++= "\\u" ++= t.substring(t.length - 4)
                        }
                        else
                            sb += ch
                }
            
            sb.toString()
        }
    }
    
    /**
      * Converts closure to a runnable.
      *
      * @param f Closure to convert.
      */
    implicit def toRun(f: ⇒ Unit): Runnable = () ⇒ try {
        f
    }
    catch {
        case _: InterruptedException ⇒ Thread.currentThread().interrupt()
        case e: Throwable ⇒ logger.error("Unhandled exception caught.", e)
    }
    
    /**
      * Destroys given process (using proper waiting algorithm).
      *
      * @param proc Process to destroy. No-op if `null`.
      */
    def destroyProcess(proc: Process): Unit = {
        if (proc != null) {
            proc.destroy()
    
            while (!proc.waitFor(100, TimeUnit.MILLISECONDS)) {
                Thread.sleep(100)
        
                proc.destroy()
            }
        }
    }
    
    /**
      * Type case with option.
      */
    def as[T: Manifest](any: Any): Option[T] = any match {
        case _: T ⇒ Some(any.asInstanceOf[T])
        case _ ⇒ None
    }
    
    /**
      * 
      * @param body Expression that can produce [[InterruptedException]].
      */
    def ignoreInterrupt(body: ⇒ Unit): Unit =
        try {
            body
        }
        catch {
            case _: InterruptedException ⇒ ()
        }
    
    /**
      * Hides private information from the given JSON string so that it can be
      * safely printed in log file, etc.
      *
      * @param json JSON string to hide private information in.
      * @return Hidden string.
      */
    def hideJsonPrivates(json: String): String = {
        if (!IS_DEBUG) {
            var s = PASSWD_REGEX.replaceAllIn(json, "\"passwd\": \"********\"")
            s = OLD_PASSWD_REGEX.replaceAllIn(s, "\"oldPassword\": \"********\"")
            s = NEW_PASSWD_REGEX.replaceAllIn(s, "\"newPassword\": \"********\"")
            s = PASSWORD_REGEX.replaceAllIn(s, "\"password\": \"********\"")
            s = LOGIN_TKN_REGEX.replaceAllIn(s, "\"loginTkn\": \"********\"")
            
            // TODO: add any other private information hiding here.
            
            s
        }
        else
            json
    }
    
    /**
      * Hides private information from the given URI string so that it can be
      * safely printed in log file, etc.
      *
      * @param uri URI string to hide private information in.
      * @return Hidden string.
      */
    def hideUriPrivates(uri: String): String = {
        if (!IS_DEBUG) {
            var s = PROBE_TKN_REGEX.replaceAllIn(uri, "probeToken=********")
            s = ACCESS_TKN_REGEX.replaceAllIn(s, "accessToken=********")
            
            // TODO: add any other private information hiding here.
            
            s
        }
        else
            uri
    }

    /**
      * Converts object's package name into path.
      */
    def toPath(a: Any): String = toPath(a.getClass)
    
    /**
      * Converts class into path.
      */
    def toPath(`class`: Class[_]): String = `class`.getPackage.getName.replaceAll("\\.", "/")
    
    /**
      * Reads lines from given file.
      *
      * @param path File path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readPath(path: String, enc: String, log: Logger = logger): Iterator[String] =
        readFile(new File(path), enc, log)

    /**
      * Reads lines from given resource.
      *
      * @param res Resource path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readResource(res: String, enc: String, log: Logger = logger): Iterator[String] =
        readStream(getStream(res), enc, log)

    /**
      * Reads lines from given file.
      *
      * @param path Zipped file path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readGzipPath(path: String, enc: String, log: Logger = logger): Iterator[String] =
        readGzipFile(new File(path), enc, log)
    
    /**
      * Reads lines from given file.
      *
      * @param f File to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readFile(f: File, enc: String, log: Logger = logger): Iterator[String] =
        try {
            val data = Source.fromFile(f, enc).getLines().map(p ⇒ p)

            log.trace(s"Loaded file: ${f.getAbsolutePath}")

            data
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read file: ${f.getAbsolutePath}", e)
        }

    /**
      * Reads lines from given stream.
      *
      * @param in Stream to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readStream(in: InputStream, enc: String, log: Logger = logger): Iterator[String] =
        try
            Source.fromInputStream(in, enc).getLines().map(p ⇒ p)
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read stream", e)
        }

    /**
      * Reads lines from given file converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param f File to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextFile(f: File, enc: String, log: Logger = logger): Iterator[String] =
    try {
        val data = Source.fromFile(f, enc).getLines().map(_.toLowerCase.trim).
            filter(s ⇒ !s.isEmpty && !s.startsWith("#"))
        
        log.trace(s"Loaded file: ${f.getAbsolutePath}")
        
        data
    }
    catch {
        case e: IOException ⇒ throw new NCE(s"Failed to read file: ${f.getAbsolutePath}", e)
    }

    /**
      * Reads lines from given stream converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param in Stream to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextStream(in: InputStream, enc: String, log: Logger = logger): Iterator[String] =
        try {
            Source.fromInputStream(in, enc).getLines().map(_.toLowerCase.trim).
                filter(s ⇒ !s.isEmpty && !s.startsWith("#"))
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read stream", e)
        }
    
    /**
      * Reads lines from given file converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param f Zipped file to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextGzipFile(f: File, enc: String, log: Logger = logger): Iterator[String] =
        try {
            val data = Source.fromInputStream(new GIS(new FileInputStream(f)), enc).getLines().map(_.toLowerCase.trim).
                filter(s ⇒ !s.isEmpty && !s.startsWith("#"))

            log.trace(s"Loaded file: ${f.getAbsolutePath}")

            data
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read file: ${f.getAbsolutePath}", e)
        }

    /**
      * Reads lines from given stream converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param res Zipped resource to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextGzipResource(res: String, enc: String, log: Logger = logger): Iterator[String] =
        try
            Source.fromInputStream(new GIS(getStream(res)), enc).getLines().map(_.toLowerCase.trim).
                filter(s ⇒ !s.isEmpty && !s.startsWith("#"))
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read stream", e)
        }

    /**
      * Reads lines from given file converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param path File path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextPath(path: String, enc: String, log: Logger = logger): Iterator[String] =
        readTextFile(new File(path), enc, log)

    /**
      * Reads lines from given resource converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param res Resource to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextResource(res: String, enc: String, log: Logger = logger): Iterator[String] =
        readTextStream(getStream(res), enc, log)
    
    /**
      * Reads lines from given file converting to lower case, trimming, and filtering
      * out empty lines and comments (starting with '#').
      *
      * @param path Zipped file path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextGzipPath(path: String, enc: String, log: Logger = logger): Iterator[String] =
        readTextGzipFile(new File(path), enc, log)
    
    /**
      *
      * @param path Folder path to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readTextFolder(path: String, enc: String, log: Logger = logger): Iterator[String] =
        (for (file ← new File(path).listFiles()) yield
            readTextFile(file, enc, logger)).toIterator.flatten
    
    /**
      * Converts given name into properly capitalized first and last name.
      *
      * @param name Full name.
      */
    def toFirstLastName(name: String): (String, String) = {
        val parts = name.trim.split(' ')
        
        val firstName = formatName(parts.head)
        val lastName = formatName(parts.tail.mkString(" "))
        
        (firstName, lastName)
    }
    
    /**
      * Properly capitalizes name (first name or last name).
      *
      * @param name First or last name.
      */
    def formatName(name: String): String = {
        name.trim.toLowerCase.capitalize
    }
    
    /**
      * Makes daily timer.
      *
      * @param name Timer name.
      * @param body Body function.
      * @param hour Hours of start.
      * @param mins Minutes of start. Optional.
      * @param secs Seconds of start. Optional.
      */
    def mkDailyTimer(name: String, body: Unit ⇒ Unit, hour: Int, mins: Int = 0, secs: Int = 0): Timer = {
        val timer = new Timer()
        
        val cal = C.getInstance()
        
        val now = cal.getTime
        
        cal.set(C.HOUR_OF_DAY, hour)
        cal.set(C.MINUTE, mins)
        cal.set(C.SECOND, secs)
        
        if (cal.getTime.before(now))
            cal.add(C.DAY_OF_YEAR, 1)
        
        val firstTime = cal.getTime
        
        val period = 24 * 60 * 60 * 1000
        
        timer.schedule(
            new TimerTask {
                override def run(): Unit = {
                    val now = System.currentTimeMillis()
                    
                    try {
                        body(())
                        
                        logger.debug(s"Timer task executed [name=$name, execution-time=${System.currentTimeMillis() - now}]")
                    }
                    catch {
                        case e: Throwable ⇒ logger.error(s"Error executing daily timer [name=$name]", e)
                    }
                }
            },
            firstTime,
            period
        )
        
        logger.trace(s"Timer started [name=$name, first-execution-time=$firstTime, period=$period]")
        
        timer
    }
    
    /**
      * Reads lines from given file.
      *
      * @param f Zipped file to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readGzipFile(f: File, enc: String, log: Logger = logger): Iterator[String] =
        try {
            val data = Source.fromInputStream(new GIS(new FileInputStream(f)), enc).getLines().map(p ⇒ p)
            
            log.trace(s"Loaded file: ${f.getAbsolutePath}")
            
            data
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read file: ${f.getAbsolutePath}", e)
        }

    /**
      *
      * @param in Zipped stream to read from.
      * @param enc Encoding.
      * @param log Logger to use.
      * @return
      */
    @throws[NCE]
    def readGzipResource(in: InputStream, enc: String, log: Logger = logger): Iterator[String] =
        try
            Source.fromInputStream(new GIS(in), enc).getLines().map(p ⇒ p)
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read stream", e)
        }
    
    /**
      * Reads bytes from given file.
      *
      * @param path File path.
      * @param log Logger.
      */
    @throws[NCE]
    def readPathBytes(path: String, log: Logger = logger): Array[Byte] =
        readFileBytes(new File(path), log)
    
    /**
      * Reads bytes from given file.
      *
      * @param f File.
      * @param log Logger.
      */
    @throws[NCE]
    def readFileBytes(f: File, log: Logger = logger): Array[Byte] = {
        try {
            val arr = new Array[Byte](f.length().toInt)
            
            managed(new FileInputStream(f)) acquireAndGet { in ⇒
                in.read(arr)
            }
            
            logger.trace(s"File read: $f")
            
            arr
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error reading file: $f", e)
        }
    }

    /**
      * Reads bytes from given file.
      *
      * @param f File to read from.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readBinaryFile(f: File, log: Logger = logger): Array[Byte] =
        try {
            val data = Source.fromFile(f, "ISO-8859-1").map(_.toByte).toArray
            
            log.trace(s"Loaded file: ${f.getAbsolutePath}")
            
            data
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Failed to read file: ${f.getAbsolutePath}", e)
        }
    
    /**
      * Reads bytes from given file.
      *
      * @param path File path to read from.
      * @param log Logger to use.
      */
    @throws[NCE]
    def readBinaryPath(path: String, log: Logger = logger): Array[Byte] =
        readBinaryFile(new File(path), log)
    
    /**
      * Gzip file.
      *
      * @param f File.
      * @param log Logger.
      */
    @throws[NCE]
    def gzipFile(f: File, log: Logger = logger): Unit = {
        val gz = s"${f.getAbsolutePath}.gz"

        // Do not user BOS here - it makes files corrupted.
        try
            managed(new GOS(new FileOutputStream(gz))) acquireAndGet { stream ⇒
                stream.write(readFileBytes(f))

                stream.flush()
            }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error gzip file: $f", e)
        }
        
        if (!f.delete())
            throw new NCE(s"Error while deleting file: $f")
        
        logger.trace(s"File gzipped [source=$f, destination=$gz]")
    }
    
    /**
      * Gzip file.
      *
      * @param path File path.
      * @param log Logger.
      */
    @throws[NCE]
    def gzipPath(path: String, log: Logger = logger): Unit = gzipFile(new File(path), log)
    
    /**
      * Generates read-only text file with given path and strings.
      * Used by text files auto-generators.
      *
      * @param path Path of the output file.
      * @param lines Text data.
      * @param sort Whether to sort output or not.
      */
    @throws[IOException]
    def mkTextFile(path: String, lines: Traversable[Any], sort: Boolean = true) {
        val file = new File(path)
        
        managed(new PrintStream(file)) acquireAndGet {
            ps ⇒
                import java.util._
                
                val year = Calendar.getInstance.get(Calendar.YEAR)
                
                // Could be long for large sequences...
                val seq =
                    if (sort)
                        lines.map(_.toString).toSeq.sorted
                    else
                        lines
                
                ps.println(s"#")
                ps.println(s"# Auto-generated on: ${new Date()}")
                ps.println(s"# Total lines: ${seq.size}")
                ps.println(s"#")
                ps.println(s"# +-------------------------+")
                ps.println(s"# | DO NOT MODIFY THIS FILE |")
                ps.println(s"# +-------------------------+")
                ps.println(s"#")
                ps.println()
                
                seq.foreach(ps.println)
                
                // Make the file as read-only.
                file.setWritable(false, false)
        }
        
        // Ack.
        println(s"File generated: $path")
    }

    /**
      * Gets resource stream from classpath.
      *
      * @param res Resource.
      */
    @throws[NCE]
    def getStream(res: String): InputStream = {
        val in = getClass.getClassLoader.getResourceAsStream(res)
        
        if (in == null)
            throw new NCE(s"Resource not found: $res")

        in
    }

    /**
      * Gets resource existing flag.
      *
      * @param res Resource.
      */
    @throws[NCE]
    def hasResource(res: String): Boolean =
        getClass.getClassLoader.getResourceAsStream(res) != null

    /**
      * Serializes data from file.
      *
      * @param path File path.
      */
    @throws[NCE]
    def serializePath(path: String, obj: Any): Unit = {
        try {
            managed(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) acquireAndGet { out ⇒
                out.writeObject(obj)
            }
            
            logger.info(s"File $path is written.")
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error writing file: $path", e)
        }
    }
    
    /**
      * Serializes data.
      *
      * @param obj Data.
      */
    @throws[NCE]
    def serialize(obj: Any): Array[Byte] = {
        try {
            managed(new ByteArrayOutputStream()) acquireAndGet { baos ⇒
                managed(new ObjectOutputStream(new BufferedOutputStream(baos))) acquireAndGet { out ⇒
                    out.writeObject(obj)
                }
                
                baos.toByteArray
            }
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error serialization data: $obj", e)
        }
    }
    
    /**
      * Serializes data from file.
      *
      * @param file File.
      */
    @throws[NCE]
    def serialize(file: File, obj: Any): Unit = {
        try {
            managed(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) acquireAndGet { out ⇒
                out.writeObject(obj)
            }
            
            logger.info(s"File $file is written.")
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error writing file: $file", e)
        }
    }
    
    /**
      * Deserializes data from file.
      *
      * @param path File path.
      */
    @throws[NCE]
    def deserializePath[T](path: String, log: Logger = logger): T =
    try {
        val res = managed(new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)))) acquireAndGet { in ⇒
            in.readObject().asInstanceOf[T]
        }
        
        log.trace(s"Read file: $path")
        
        res
    }
    catch {
        case e: IOException ⇒ throw new NCE(s"Error reading file: $path", e)
    }
    
    /**
      * Deserializes data.
      *
      * @param arr File path.
      */
    @throws[NCE]
    def deserialize[T](arr: Array[Byte]): T =
        try {
            managed(new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(arr)))) acquireAndGet { in ⇒
                in.readObject().asInstanceOf[T]
            }
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error deserialization data", e)
        }
    
    /**
      * Deserializes data from file.
      *
      * @param f File.
      * @param log Logger.
      */
    @throws[NCE]
    def deserialize[T](f: File, log: Logger = logger): T =
        try {
            val res = managed(new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) acquireAndGet { in ⇒
                in.readObject().asInstanceOf[T]
            }
            
            log.trace(s"Read file: ${f.getAbsolutePath}")
            
            res
        }
        catch {
            case e: IOException ⇒ throw new NCE(s"Error reading file: $f", e)
        }
    
    /**
      * Wrap string value.
      *
      * @param s String value.
      */
    def wrapQuotes(s: String): String =
        s""""$s""""
    
    /**
      * Recursively removes all files and nested directories in a given folder.
      * Provided root folder itself is not removed.
      *
      * @param rootDir Folder to remove all nested files and directories in it.
      */
    @throws[IOException]
    def clearFolder(rootDir: String) {
        val rootPath = Paths.get(rootDir)
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor[Path] {
            private def delete(path: Path) = {
                Files.delete(path)
                
                FileVisitResult.CONTINUE
            }
            
            override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult =
                if (e == null)
                    if (!dir.equals(rootPath))
                        delete(dir)
                    else
                        FileVisitResult.CONTINUE
                else
                    throw e
            
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = delete(file)
        })
    }
    
    /**
      * Convenient utility to create future with given body and optional callbacks and execution context.
      *
      * @param body Body.
      * @param onFailure On error optional callback. No-op if not provided.
      * @param onSuccess On success optional callback. No-op if not provided.
      * @param ec Optional execution context. If not provided - the default Scala execution context will be used.
      */
    def asFuture[T](
        body: Unit ⇒ T,
        onFailure: Throwable ⇒ Unit = _ ⇒ Unit,
        onSuccess: T ⇒ Unit = (_: T) ⇒ ())(implicit ec: ExecutionContext = global): Future[T] = {
        val fut = Future { body(()) }
        
        fut.onFailure {
            case e: Throwable ⇒ onFailure(e)
        }
        fut.onSuccess {
            case t ⇒ onSuccess(t)
        }
        
        fut
    }
    
    /**
      * Makes thread.
      *
      * @param name Name.
      * @param body Thread body.
      */
    def mkThread(name: String)(body: Thread ⇒ Unit): Thread =
        new Thread(name) {
            @volatile private var stopped = false
            
            override def isInterrupted: Boolean = super.isInterrupted || stopped
            
            override def interrupt(): Unit = {
                super.interrupt()
                
                stopped = true
            }
            
            override def run(): Unit = {
                logger.trace(s"Thread started: $name")
                
                try {
                    body(this)
                    
                    logger.trace(s"Thread exited: $name")
                }
                catch {
                    case _: InterruptedException ⇒ logger.trace(s"Thread interrupted: $name")
                    case e: Throwable ⇒ logger.error(s"Unexpected error during thread execution: $name", e)
                }
                finally {
                    stopped = true
                }
            }
        }
    
    /**
      * System-wide process of normalizing emails (trim & lower case).
      *
      * @param email Email to normalize.
      */
    def normalizeEmail(email: String): String = email.trim.toLowerCase
    
    /**
      * Makes size restricted synchronized map.
      */
    def mkLRUMap[K, V](name: String, maxSize: Int): java.util.Map[K, V] =
        java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap[K, V]() {
                override def removeEldestEntry(eldest: java.util.Map.Entry[K, V]): Boolean = {
                    val b = size() > maxSize
                    
                    if (b)
                        logger.warn(s"Map is too big (removing LRU item) [" +
                            s"name=$name, " +
                            s"max-size=$maxSize" +
                            s"]"
                        )
                    
                    b
                }
            }
        )

    /**
      * Gets system property, or environment variable (in that order), or `None` if none exists.
      *
      * @param s Name of the system property or environment variable.
      */
    def sysEnv(s: String): Option[String] =
        sysProps.get(s).orElse(sys.env.get(s))
    
    
    /**
      * Gets random value from given sequence.
      *
      * @param seq Sequence.
      */
    def getRandom[T](seq: Seq[T]): T = seq(RND.nextInt(seq.size))
    
    
    /**
      * Makes random filled sequence with given length from initial.
      *
      * @param seq Initial sequence.
      * @param n Required sequence length.
      */
    def getRandomSeq[T](seq: Seq[T], n: Int): Seq[T] = {
        require(seq.lengthCompare(n) >= 0)
        
        val src = scala.collection.mutable.ArrayBuffer.empty[T] ++ seq
        val dest = scala.collection.mutable.ArrayBuffer.empty[T]
        
        (0 until n).foreach(_ ⇒ dest += src.remove(RND.nextInt(src.size)))
        
        dest
    }
    
    /**
      * Gets system property, or environment variable (in that order), or throws exception if none exists.
      *
      * @param s Name of the system property or environment variable.
      */
    @throws[NCE]
    def mandatorySysEnv(s: String): String =
        sysEnv(s) match {
            case Some(v) ⇒ v
            case None ⇒ throw new NCE(s"Cannot find environment variable or system property: $s")
        }
    
    /**
      * Compresses given string.
      *
      * @param rawStr String to compress.
      * @return Compressed Base64-encoded string.
      */
    def compress(rawStr: String): String = {
        val arr = new ByteArrayOutputStream(1024)
        
        managed(new GOS(arr)) acquireAndGet { zip ⇒
            zip.write(rawStr.getBytes)
        }
        
        Base64.encodeBase64String(arr.toByteArray)
    }
    
    /**
      * Uncompresses given Base64-encoded previously compressed string.
      *
      * @param zipStr Compressed string.
      * @return Uncompressed string.
      */
    def uncompress(zipStr: String): String =
        IOUtils.toString(new GIS(new ByteArrayInputStream(Base64.decodeBase64(zipStr))), Charset.defaultCharset())
    
    /**
      * Pimps integers with KB, MB, GB units of measure.
      *
      * @param v Integer value.
      */
    implicit class IntMemoryUnits(v: Int) {
        def TB: Int = v * 1024 * 1024 * 1024 * 1024
        def GB: Int = v * 1024 * 1024 * 1024
        def MB: Int = v * 1024 * 1024
        def KB: Int = v * 1024
        
        def tb: Int = TB
        def gb: Int = GB
        def mb: Int = MB
        def kb: Int = KB
    }
    
    /**
      * Pimps longs with KB, MB, GB units of measure.
      *
      * @param v Long value.
      */
    implicit class LongMemoryUnits(v: Long) {
        def TB: Long = v * 1024 * 1024 * 1024 * 1024
        def GB: Long = v * 1024 * 1024 * 1024
        def MB: Long = v * 1024 * 1024
        def KB: Long = v * 1024
        
        def tb: Long = TB
        def gb: Long = GB
        def mb: Long = MB
        def kb: Long = KB
    }
    
    
    /**
      * Pimps integers with time units.
      *
      * @param v Integer value.
      */
    implicit class IntTimeUnits(v: Int) {
        def MSECS: Int = v
        def MS: Int = v
        def SECS: Int = v * 1000
        def MINS: Int = v * 1000 * 60
        def HOURS: Int = v * 1000 * 60 * 60
        def DAYS: Int = v * 1000 * 60 * 60 * 24
        
        def msecs: Int = MSECS
        def ms: Int = MS
        def secs: Int = SECS
        def mins: Int = MINS
        def hours: Int = HOURS
        def days: Int = DAYS
    }
    
    /**
      * Pimps long with time units.
      *
      * @param v Long value.
      */
    implicit class LongTimeUnits(v: Long) {
        def MSECS: Long = v
        def MS: Long = v
        def SECS: Long = v * 1000
        def MINS: Long = v * 1000 * 60
        def HOURS: Long = v * 1000 * 60 * 60
        def DAYS: Long = v * 1000 * 60 * 60 * 24
        
        def msecs: Long = MSECS
        def ms: Long = MS
        def secs: Long = SECS
        def mins: Long = MINS
        def hours: Long = HOURS
        def days: Long = DAYS
    }
    
    /**
      * Sleeps number of msec properly handling exceptions.
      *
      * @param delay Number of msec to sleep.
      */
    def sleep(delay: Long): Unit =
        try
            Thread.sleep(delay)
        catch {
            case _: InterruptedException ⇒ Thread.currentThread().interrupt()
            case e: Throwable ⇒ logger.error("Unhandled exception caught during sleep.", e)
        }
    
    /**
      * Interrupts thread and waits for its finish.
      *
      * @param t Thread.
      */
    def stopThread(t: Thread): Unit =
        if (t != null) {
            t.interrupt()
            
            try
                t.join()
            catch {
                case _: InterruptedException ⇒ logger.trace("Thread joining was interrupted (ignoring).")
            }
        }
    
    /**
      * Shuts down executor service and waits for its finish.
      *
      * @param es Executor service.
      */
    def shutdownPool(es: ExecutorService): Unit =
        if (es != null) {
            es.shutdown()
            es.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
        }
    
    /**
      * Gets full path for given file name in user's home folder.
      *
      * @param file File name.
      */
    def homeFileName(file: String): String = new File(System.getProperty("user.home"), file).getAbsolutePath
    
    /**
      * Non Empty Or Null (NEON).
      *
      * @param s String to check.
      */
    def neon(s: String): Boolean = s != null && !s.isEmpty
    
    /**
      * Generates new Global Unique Identifier (GUID).
      * Note that this is not compatible with Ignite-generated UUIDs.
      */
    def genGuid(): String = UUID.randomUUID().toString.toUpperCase()
    
    /**
      * Converts non-empty sequence of '\n' and '\s' into one ' '.
      *
      * @param s Object to remove spaces from.
      */
    def zipSpaces(s: AnyRef): String = s.toString.replaceAll( """[\n\s]+""", " ")
    
    /**
      * Pimps `Option[T]` with `getOrFail` function that improves on standard
      * `get` by adding user-defined descriptive error message in case of `None`.
      *
      * @param opt Option to pimp.
      */
    implicit class GetOrFail[T](val opt: Option[T]) extends AnyVal {
        @throws[NCE]
        def getOrFail(errMsg: String): T = if (opt.isDefined) opt.get else throw new NCE(errMsg)
    }
    
    /**
      * Checks duplicated elements in collection.
      *
      * @param seq Collection.
      * @param seen Checked elements.
      */
    @annotation.tailrec
    def containsDups[T](seq: List[T], seen: Set[T] = Set[T]()): Boolean =
        seq match {
            case x :: xs ⇒ if (seen.contains(x)) true else containsDups(xs, seen + x)
            case _ ⇒ false
        }
    
    /**
      * Gets a sequence without dups. It works by checking for dups first, before creating a new
      * sequence if dups are found. It's more efficient when dups are rare.
      *
      * @param seq Sequence with potential dups.
      */
    def distinct[T](seq: List[T]): List[T] =
        if (containsDups(seq))
            seq.distinct
        else
            seq
    
    /**
      * Safely and silently closes the client socket.
      *
      * @param sock Client socket to close.
      */
    def close(sock: Socket): Unit =
        if (sock != null)
            ignoring(classOf[IOException]) {
                sock.close()
            }
    
    /**
      * Safely and silently closes the server socket.
      *
      * @param sock Server socket to close.
      */
    def close(sock: ServerSocket): Unit =
        if (sock != null)
            ignoring(classOf[IOException]) {
                sock.close()
            }
    
    /**
      *
      * @param in Stream.
      */
    def close(in: InputStream): Unit =
        if (in != null)
            ignoring(classOf[IOException]) {
                in.close()
            }
    
    /**
      *
      * @param out Stream.
      */
    def close(out: OutputStream): Unit =
        if (out != null)
            ignoring(classOf[IOException]) {
                out.close()
            }
    
    /**
      * Closes auto-closeable ignoring any exceptions.
      *
      * @param a Resource to close.
      */
    def close(a: AutoCloseable): Unit =
        if (a != null)
            ignoring(classOf[Exception]) {
                a.close()
            }
    
    
    /**
      * Formats given double number with provided precision.
      *
      * @param num Number to format.
      * @param precision Number of digits after decimal point.
      */
    def format(num: Double, precision: Int): String = precision match {
        case 0 ⇒ DEC_FMT0.format(num)
        case 1 ⇒ DEC_FMT1.format(num)
        case _ ⇒ DEC_FMT2.format(num)
    }

    /**
      * Unzips file.
      *
      * @param zipFile Zip file.
      * @param outDir Output folder.
      */
    @throws[NCE]
    def unzip(zipFile: String, outDir: String): Unit = {
        @throws[NCE]
        def mkDir(dir: File): Unit =
            if (dir != null && !dir.exists()) {
                if (!dir.mkdirs())
                    throw new NCE(s"Folder cannot be created: ${dir.getAbsolutePath}")
            }

        mkDir(new File(outDir))

        managed(new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) acquireAndGet { in ⇒
            var entry = in.getNextEntry

            while (entry != null) {
                val f = new File(outDir, entry.getName)

                if (!entry.isDirectory) {
                    mkDir(f.getParentFile)

                    try {
                        if (!f.createNewFile())
                            throw new NCE(s"File cannot be created: ${f.getAbsolutePath}")

                        managed(new BufferedOutputStream(new FileOutputStream(f))) acquireAndGet { out ⇒
                            IOUtils.copy(in, out)
                        }
                    }
                    catch {
                        case e: IOException ⇒ throw new NCE(s"IO error processing file: ${f.getAbsolutePath}.", e)
                    }
                }

                entry = in.getNextEntry
            }
        }
    }

    /**
      * Tokenize string splitting by space.
      *
      * @param s String for tokenization.
      */
    def tokenizeSpace(s: String): Seq[String] = s.split(" ")

    /**
      * Makes SHA256 hash.
      * 
      * @param s String.
      */
    def makeSha256Hash(s: String): String = DigestUtils.sha256Hex(s)

    /**
      * Makes properties file based on input string.
      *
      * @param s String.
      */
    @throws[NCE]
    def makeProperties(s: String): Properties = {
        val p = new Properties()

        try
            p.load(new StringReader(s))
        catch {
            case e: IOException ⇒ throw new NCE(s"Error reading properties: $s", e)
        }

        p
    }

    /**
      * Converts error with its trace to string.
      *
      * @param t Error.
      */
    def toString(t: Throwable): String  =
        managed(new ByteArrayOutputStream()) acquireAndGet { out ⇒
            managed(new PrintStream(out)) acquireAndGet { ps ⇒
                t.printStackTrace(ps)

                new String(out.toByteArray, "UTF8")
            }
        }

    /**
      * Makes absolute path starting from working directory.
      *
      * @param path Path.
      */
    def mkPath(path: String): String = new File(s"${new File("").getAbsolutePath}/$path").getAbsolutePath

    /**
      * Gets either environment variable or system property based path with given name.
      *
      * @param s Environment variable or system property name.
      */
    @throws[NCE]
    def getSysEnvPath(s: String): String = {
        // NOTE: system property overrides environment variable.
        val v = G.mandatorySysEnv(s)

        if (!new File(v).exists())
            throw new NCE(s"Path '$v' does not exist.")

        v
    }

    /**
      * Gets resources from resources folder.
      * based on http://www.uofr.net/~greg/java/get-resource-listing.html
      *
      * @param resDir Folder.
      */
    def getFilesResources(resDir: String): Seq[String] = {
        val clazz = getClass

        val url = {
            val x = clazz.getClassLoader.getResource(resDir)

            if (x != null) x else clazz.getClassLoader.getResource(clazz.getName.replace(".", "/") + ".class")
        }

        url.getProtocol match {
            case "file" ⇒
                managed(new InputStreamReader(getStream(resDir))) acquireAndGet { reader ⇒
                    managed(new BufferedReader(reader)) acquireAndGet { bReader ⇒
                        bReader.lines().collect(Collectors.toList[String]).asScala.map(p ⇒ s"$resDir/$p")
                    }
                }
            case "jar" ⇒
                val jar = new JarFile(URLDecoder.decode(url.getPath.substring(5, url.getPath.indexOf("!")), "UTF-8"))
                val entries = jar.entries

                val res = mutable.ArrayBuffer.empty[String]

                while (entries.hasMoreElements) {
                    val name = entries.nextElement.getName

                    if (name.startsWith(resDir) && name != s"$resDir/")
                        res += name
                }

                res
            case _ ⇒ throw new NCE(s"Cannot list files for: $resDir")
        }
    }

    /**
      * Splits endpoints into host and port.
      *
      * @param ep Endpoint to split.
      * @return Host and port tuple.
      */
    @throws[NCE]
    def splitEndpoint(ep: String): (String, Int) = {
        val idx = ep.indexOf(':')

        if (idx == -1)
            throw new NCE(s"Invalid uplink endpoint: $ep")
        else
            ep.substring(0, idx) → ep.substring(idx + 1).toInt
    }
}
