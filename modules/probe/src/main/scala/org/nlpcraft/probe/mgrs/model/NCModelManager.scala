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

package org.nlpcraft.probe.mgrs.model

import java.util.regex.{Pattern, PatternSyntaxException}
import java.util.{Collection ⇒ JCollection, Set ⇒ JSet}

import org.nlpcraft._
import org.nlpcraft.ascii.NCAsciiTable
import org.nlpcraft.makro.{NCMacroParser ⇒ MacroParser}
import org.nlpcraft.mdllib._
import org.nlpcraft.nlp.opennlp.NCNlpManager
import org.nlpcraft.nlp.pos._
import org.nlpcraft.probe.NCSynonymChunkKind._
import org.nlpcraft.probe._
import org.nlpcraft.probe.mgrs.deploy._
import org.nlpcraft.probe.usage._

import scala.collection.JavaConversions._
import scala.collection.convert.DecorateAsScala
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Exception._

/**
  * Model manager.
  */
object NCModelManager extends NCProbeManager("PROBE model manager") with NCDebug with DecorateAsScala {
    // All possible element types.
    private final val ELM_TYPES = Set(
        "STRING",
        "DATE",
        "DATETIME",
        "LONG",
        "DOUBLE",
        "BOOLEAN",
        "DATE"
    )
    
    // Deployed models keyed by their IDs.
    private val models = mutable.HashMap.empty[String, NCModelDecorator]
    // Model usage keyed by model IDs.
    private val usages = mutable.HashMap.empty[String, NCModelUsage]
    
    // Access mutex.
    private final val mux = new Object()
    
    /**
      *
      * @param elementId Element ID.
      * @param synonym Element synonym.
      */
    case class SynonymHolder(
        elementId: String,
        synonym: NCSynonym
    )
    
    /**
      * Tokenizes model element ID or synonym value name that act as **implicit**
      * synonyms. It tries to mimic Stanford's PTBTokenizer which is unavailable on the probe itself.
      *
      * @param s Either model element ID or synonym value name.
      * @param group Whether or not to group non-letter or digit characters into one token.
      * @return
      */
    private[model] def tokenize(s: String, group: Boolean): Seq[String] = {
        val len = s.length()
        var tokBuf = new StringBuilder()
        var i = 0
        val toks = ArrayBuffer.empty[String]
        var f = false
        
        def addToken(): Unit = {
            val x = tokBuf.toString.trim
    
            if (x.nonEmpty)
                toks += x
    
            tokBuf = new StringBuilder()
        }
        
        while (i < len) {
            val ch = s.charAt(i)
            
            if (!ch.isLetterOrDigit) {
                if ((group && !f) || !group)
                    addToken()
                
                f = true
            }
            else {
                if (f)
                    addToken()
                
                f = false
            }
            
            tokBuf += ch
        
            i += 1
        }
    
        addToken()
    
        var isPrevTick = false
        
        // Fix special case of handling "'s" by CoreNLP.
        val x = for (tok ← toks.filter(_.nonEmpty)) yield {
            if (tok.toLowerCase == "s" && isPrevTick) {
                isPrevTick = false
                Some("'s")
            }
            else if (tok == "'") {
                isPrevTick = true
                None
            }
            else {
                isPrevTick = false
                Some(tok)
            }
        }
        
        x.flatten
    }
    
    /**
      * @param provider Model provider.
      * @param id Model ID.
      */
    private def addNewModel(provider: NCModelProvider, id: String): Boolean = {
        require(Thread.holdsLock(mux))
    
        if (id == null)
            false
        else {
            val mdl = provider.makeModel(id)
            
            if (mdl != null) {
                try {
                    checkModelConfig(mdl)
                    
                    val parser = new MacroParser
                    val macros = mdl.getMacros

                    // Initialize macro parser.
                    if (macros != null)
                        macros.asScala.foreach(t ⇒ parser.addMacro(t._1, t._2))
                    
                    val dec = verifyAndDecorate(mdl, parser)
                    val usage = makeModelUsage(mdl, parser)
                    
                    mdl.initialize(new NCProbeContext {
                        override def reloadModel(modelId: String): Unit = new Thread() {
                            override def run(): Unit =reload(modelId)
                        }.start()
                        
                        override lazy val getId: String = config.getId
                        override lazy val getToken: String = config.getToken
                        override lazy val getUpLink: String = config.getUpLink
                        override lazy val getDownLink: String = config.getDownLink
                        override lazy val getEmail: String = config.getEmail
                        override lazy val getJarsFolder: String = config.getJarsFolder
                    })
                    
                    models += id → dec
                    usages += id → usage
                }
                catch {
                    case e: NCE ⇒
                        logger.error(s"Model '${if (id == null) "null" else id}' validation error: ${e.getMessage}")
                }
                true
            }
            else
                false
        }
    }
    
    /**
      * Starts this component.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        mux.synchronized {
            for (provider ← NCDeployManager.getProviders)
                for (ds ← NCDeployManager.getDescriptors)
                    addNewModel(provider, ds.getId)
            
            val tbl = NCAsciiTable("Model ID", "Name", "Ver.", "Elements", "Synonyms")
            
            models.values.foreach(m ⇒ {
                val ds = m.model.getDescriptor
                val synCnt = m.synonyms.values.flatMap(_.values).flatten.size
                
                tbl += (ds.getId, ds.getName, ds.getVersion, m.elements.keySet.size, synCnt)
            })
            
            tbl.info(logger, Some(s"Models deployed: ${models.size}\n"))
            
            if (models.isEmpty && config.getJarsFolder == null)
                throw new NCException("No models deployed and no JAR folder specified.")
        }
        
        super.start()
    }
    
    /**
      *
      * @param mdl
      */
    private def discardModel(mdl: NCModel): Unit = {
        require(Thread.holdsLock(mux))
        
        ignoring(classOf[Throwable]) {
            // Ack.
            logger.info(s"Model discarded: ${mdl.getDescriptor.getId}")
            
            mdl.discard()
        }
    }
    
    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        mux.synchronized {
            models.values.foreach(m ⇒ discardModel(m.model))
        }
        
        super.stop()
    }
    
    /**
      *
      * @param fix Prefix and suffix.
      * @param s String to search prefix and suffix in.
      * @return
      */
    private def startsAndEnds(fix: String, s: String): Boolean = s.startsWith(fix) && s.endsWith(fix)

    /**
      *
      * @param s
      * @return
      */
    @throws[NCE]
    private def chunkSplit(s: String): Seq[NCSynonymChunk] = {
        val x = s.trim()
        
        if (startsAndEnds("///", x))
            Seq(mkChunk(x)) // Defensively grab entire string in case of regex.
        else
            x.split(" ").map(_.trim).filter(_.nonEmpty).map(mkChunk)
    }
    
    /**
      *
      * @param chunk Synonym chunk.
      * @return
      */
    @throws[NCE]
    private def mkChunk(chunk: String): NCSynonymChunk = {
        def stripBody(s: String): String = s.slice(3, s.length - 3)
    
        // Regex synonym.
        if (startsAndEnds("///", chunk)) {
            val ptrn = stripBody(chunk)
            
            if (ptrn.length > 0)
                try
                    NCSynonymChunk(kind = REGEX, origText = chunk, regex = Pattern.compile(ptrn))
                catch {
                    case e: PatternSyntaxException ⇒ throw new NCE(s"Invalid regex syntax in: $chunk", e)
                }
            else
                throw new NCE(s"Empty regex synonym detected: $chunk")
        }
        // POS tag synonym.
        else if (startsAndEnds("```", chunk)) {
            val tag = stripBody(chunk).toUpperCase
            
            if (NCPennTreebank.contains(tag))
                NCSynonymChunk(kind = POS, origText = chunk, posTag = tag)
            else
                throw new NCE(s"Invalid PoS synonym: $chunk")
        }
        // Regular word.
        else
            NCSynonymChunk(kind = TEXT, origText = chunk, wordStem = NCNlpManager.stem(chunk))
    }
    
    /**
      *
      * @param ds
      */
    @throws[NCE]
    private def checkModelDescriptor(ds: NCModelDescriptor): Unit = {
        if (ds == null)
            throw new NCE(s"Model descriptor is not provided.")
        
        val id = ds.getId
        val name = ds.getName
        val ver = ds.getVersion
        
        if (id == null)
            throw new NCE(s"Model descriptor ID is not provided.")
        if (name == null)
            throw new NCE(s"Model descriptor name is not provided.")
        if (ver == null)
            throw new NCE(s"Model descriptor version is not provided.")
        
        if (name.length > 64)
            throw new NCE(s"Model descriptor name is too long (64 chars max): $name")
        if (ver.length > 16)
            throw new NCE(s"Model descriptor version is too long (16 chars max): $name")
    }
    
    /**
      *
      * @param adds Additional stopword stems.
      * @param excls Excluded stopword stems.
      */
    @throws[NCE]
    private def checkStopwordsDups(adds: Set[String], excls: Set[String]): Unit = {
        val cross = adds.intersect(excls)
    
        if (cross.nonEmpty)
            throw new NCE(s"Duplicate stems in additional and excluded stopwords: '${cross.mkString(",")}'")
    }
    
    /**
      * Verifies given model and makes a decorator optimized for model enricher.
      * 
      * @param mdl Model to verify and decorate.
      * @param parser Initialized macro parser.
      * @return Model decorator.
      */
    @throws[NCE]
    private def verifyAndDecorate(mdl: NCModel, parser: MacroParser): NCModelDecorator = {
        checkModelDescriptor(mdl.getDescriptor)
    
        for (elm ← mdl.getElements)
            checkElement(mdl, elm)
    
        checkElementIdsDups(mdl)
        checkCyclicDependencies(mdl)
        checkTriviaDups(mdl, parser)
    
        val addStopWords = checkAndStemmatize(mdl.getAdditionalStopWords, "Additional stopword")
        val exclStopWords = checkAndStemmatize(mdl.getExcludedStopWords, "Excluded stopword")
        val suspWords = checkAndStemmatize(mdl.getSuspiciousWords, "Suspicious word")
    
        checkStopwordsDups(addStopWords, exclStopWords)
        
        val syns = mutable.HashSet.empty[SynonymHolder]
        val exclSyns = mutable.HashSet.empty[SynonymHolder]

        var cnt = 0
        val maxCnt = mdl.getMaxTotalSynonyms

        // Process and check elements.
        for (elm ← mdl.getElements) {
            val elmId = elm.getId
    
            def addSynonym(
                col: mutable.HashSet[SynonymHolder],
                isElementId: Boolean,
                isValueName: Boolean,
                value: String,
                chunks: Seq[NCSynonymChunk]): Unit = {
                require(col == syns || col == exclSyns) // This is internal closure.

                val kind = if (col == syns) "Synonym" else "Excluded synonym"

                def add(chunks: Seq[NCSynonymChunk], isDirect: Boolean): Unit = {
                    val holder = SynonymHolder(
                        elementId = elmId,
                        synonym = NCSynonym(isElementId, isValueName, isDirect, value, chunks)
                    )

                    if (col.add(holder)) {
                        cnt += 1

                        if (cnt > maxCnt)
                            throw new NCE(s"Too many synonyms detected [" +
                                s"model=${mdl.getDescriptor.getId}, " +
                                s"max=$maxCnt" +
                            s"]")

                        if (!IS_PROBE_SILENT) {
                            if (value == null)
                                logger.trace(s"$kind #${col.size} added [" +
                                    s"model=${mdl.getDescriptor.getId}, " +
                                    s"elementId=$elmId, " +
                                    s"synonym=${chunks.mkString(" ")}" +
                                    s"]")
                            else
                                logger.trace(s"$kind #${col.size} added [" +
                                    s"model=${mdl.getDescriptor.getId}, " +
                                    s"elementId=$elmId, " +
                                    s"synonym=${chunks.mkString(" ")}, " +
                                    s"value=$value" +
                                    s"]")
                        }
                    }
                    else if (!IS_PROBE_SILENT)
                        logger.warn(
                            s"$kind already added (ignoring) [" +
                                s"model=${mdl.getDescriptor.getId}, " +
                                s"elementId=$elmId, " +
                                s"synonym=${chunks.mkString(" ")}, " +
                                s"value=$value" +
                                s"]"
                        )
                }

                if (mdl.isPermutateSynonyms && !isElementId && !isValueName && chunks.forall(_.wordStem != null))
                    // Permutes and drops duplicated.
                    chunks.permutations.map(p ⇒ p.map(_.wordStem) → p).toMap.unzip._2.foreach(p ⇒ add(p, p == chunks))
                else
                    add(chunks, true)
            }

            def chunk0(s: String, group:Boolean): Seq[NCSynonymChunk] = chunkSplit(tokenize(s, group).mkString(" "))
    
            // Add element ID as a synonyms (Duplications ignored)
            val idChunks = Seq(chunk0(elmId, true), chunk0(elmId, false), chunkSplit(elmId))

            idChunks.distinct.foreach(ch ⇒ addSynonym(syns, true, false, null, ch))

            // Add straight element synonyms (Duplications printed as warnings)
            val synsChunks = for (syn ← elm.getSynonyms.flatMap(parser.expand)) yield chunkSplit(syn)

            if (!IS_PROBE_SILENT && G.containsDups(synsChunks.toList))
                logger.warn(s"Element synonyms duplicate (ignoring) [" +
                    s"model=${mdl.getDescriptor.getId}, " +
                    s"elementId=$elmId, " +
                    s"synonym=${synsChunks.diff(synsChunks.distinct).distinct.map(_.mkString(",")).mkString(";")}" +
                    s"]"
                )

            synsChunks.distinct.foreach(ch ⇒ addSynonym(syns, false, false, null, ch))

            // Add value synonyms.
            val valNames = elm.getValues.map(_.getName)

            if (!IS_PROBE_SILENT && G.containsDups(valNames.toList))
                logger.warn(s"Element values names duplicate (ignoring) [" +
                    s"model=${mdl.getDescriptor.getId}, " +
                    s"elementId=$elmId, " +
                    s"names=${valNames.diff(valNames.distinct).distinct.mkString(",")}" +
                    s"]"
                )

            for (v ← elm.getValues.asScala.map(p ⇒ p.getName → p).toMap.unzip._2) {
                val valName = v.getName
                val valSyns = v.getSynonyms.asScala

                val vNamesChunks = Seq(chunk0(valName, true), chunk0(valName, false), chunkSplit(valName))

                // Add value name as a synonyms (duplications ignored)
                vNamesChunks.distinct.foreach(ch ⇒ addSynonym(syns, false, true, valName, ch))

                // Add straight value synonyms (duplications printed as warnings)
                var skippedOneLikeName = false

                val vChunks =
                    valSyns.flatMap(parser.expand).flatMap(valSyn ⇒ {
                        val valSyns = chunkSplit(valSyn)

                        if (vNamesChunks.contains(valSyns) && !skippedOneLikeName) {
                            skippedOneLikeName = true

                            None
                        }
                        else
                            Some(valSyns)
                    })

                if (!IS_PROBE_SILENT && G.containsDups(vChunks.toList))
                    logger.warn(s"Element synonyms duplicate (ignoring) [" +
                        s"model=${mdl.getDescriptor.getId}, " +
                        s"elementId=$elmId, " +
                        s"value=$valName, " +
                        s"synonym=${vChunks.diff(vChunks.distinct).distinct.map(_.mkString(",")).mkString(";")}" +
                        s"]"
                    )

                vChunks.distinct.foreach(ch ⇒ addSynonym(syns, false, false, valName, ch))
            }

            // Add excluded synonyms (Duplications printed as warnings)
            val exclChunks = for (syn ← elm.getExcludedSynonyms.flatMap(parser.expand)) yield chunkSplit(syn)

            if (!IS_PROBE_SILENT && G.containsDups(exclChunks.toList))
                logger.warn(s"Element exclude synonyms duplicate (ignoring) [" +
                    s"model=${mdl.getDescriptor.getId}, " +
                    s"elementId=$elmId, " +
                    s"exclude=${exclChunks.diff(exclChunks.distinct).distinct.map(_.mkString(",")).mkString(";")}" +
                    s"]"
                )

            exclChunks.distinct.foreach(ch ⇒ addSynonym(exclSyns, false, false, null, ch))
        }
        
        var foundDups = false
        
        // Check for synonym dups across all elements.
        for (
            ((syn, isDirect), holders) ←
                syns.groupBy(p ⇒ (p.synonym.mkString(" "), p.synonym.isDirect)) if holders.size > 1 && isDirect
        ) {
            logger.warn(s"Duplicate synonym stem detected [" +
                s"model=${mdl.getDescriptor.getId}, " +
                s"element=${holders.map(
                    p ⇒ s"id=${p.elementId}${if (p.synonym.value == null) "" else s", value=${p.synonym.value}"}"
                ).mkString("(", ",", ")")}, " +
                s"synonym=$syn" +
                s"]")
            
            foundDups = true
        }
        
        if (foundDups) {
            if (!mdl.isDupSynonymsAllowed)
                throw new NCException("Duplicated synonyms detected. Check warnings messages.")

            logger.warn(s"|- Duplicate synonyms can be resolved via element excluded synonyms.")
            logger.warn(s"|- Unresolved duplicate synonyms will result in unspecified element selection.")
        }

        /**
          * 
          * @param set
          * @return
          */
        def mkFastAccessMap(set: Set[SynonymHolder]): Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]] =
            set
                .groupBy(_.elementId)
                .map {
                    case (elmId, holders) ⇒ (
                        elmId,
                        holders
                            .map(_.synonym)
                            .groupBy(_.size)
                            .map {
                                // Sort synonyms from most important to least important.
                                case (k, v) ⇒ (k, v.toSeq.sorted.reverse)
                            }
                    )
                }
    
        NCModelDecorator(
            model = mdl,
            triviaStems = generateTrivia(mdl, parser),
            synonyms = mkFastAccessMap(syns.toSet),
            excludedSynonyms = mkFastAccessMap(exclSyns.toSet),
            additionalStopWordsStems = addStopWords,
            excludedStopWordsStems = exclStopWords,
            suspiciousWordsStems = suspWords,
            elements = mdl.getElements.map(elm ⇒ (elm.getId, elm)).toMap
        )
    }
    
    /**
      * 
      * @param jc
      * @param name
      * @return
      */
    private def checkAndStemmatize(jc: JSet[String], name: String): Set[String] =
        for (word: String ← jc.asScala.toSet) yield
            if (hasWhitespace(word))
                throw new NCE(s"$name cannot have whitespace: '$word'")
            else
                NCNlpManager.stem(word)
    
    /**
      * Checks cyclic child-parent dependencies.
      *
      * @param mdl Model.
      */
    @throws[NCE]
    private def checkCyclicDependencies(mdl: NCModel): Unit =
        for (elm ← mdl.getElements) {
            if (elm.getParentId != null) {
                val seen = mutable.ArrayBuffer.empty[String]
                
                var parentId: String = null
                var x = elm
                
                do {
                    parentId = x.getParentId
                    
                    if (parentId != null) {
                        if (seen.contains(parentId))
                            throw new NCE(s"Cycling parent dependency starting at model element '${x.getId}'.")
                        else {
                            seen += parentId
                            
                            x = mdl.getElements.find(_.getId == parentId) getOrElse {
                                throw new NCE(s"Unknown parent ID '$parentId' for model element '${x.getId}'.")
                                
                                null
                            }
                        }
                    }
                }
                while (parentId != null)
            }
        }
    
    /**
      *
      * @param mdl Model.
      * @param parser Macro parser.
      */
    @throws[NCE]
    private def checkTriviaDups(mdl: NCModel, parser: MacroParser): Unit = {
        val trivia = mdl.getTrivia
        
        if (trivia.nonEmpty) {
            val grps = ArrayBuffer.empty[List[String]]
            
            // Expand trivia inputs.
            trivia.foreach(grp ⇒ {
                val ins = ArrayBuffer.empty[String]
                
                for (in ← grp.getInputs)
                    ins ++= parser.expand(in).map(NCNlpManager.stem)
                
                if (ins.nonEmpty)
                    grps += ins.toList
            })
            
            // Check for input dups in different groups.
            for (grp ← grps; in ← grp) {
                for (g ← grps if g != grp)
                    if (g.contains(in))
                        throw new NCE(s"Duplicate (expanded) trivia input '$in'.")
            }
        }
    }
    
    /**
      *
      * @param mdl Model.
      */
    @throws[NCE]
    private def checkElementIdsDups(mdl: NCModel): Unit = {
        val ids = mutable.HashSet.empty[String]
        
        for (id ← mdl.getElements.toList.map(_.getId))
            if (ids.contains(id))
                throw new NCE(s"Duplicate model element ID '$id'.")
            else
                ids += id
    }
    
    /**
      * Verifies model element in isolation.
      *
      * @param mdl Model.
      * @param elm Element to verify.
      */
    @throws[NCE]
    private def checkElement(mdl: NCModel, elm: NCElement): Unit = {
        if (elm.getId == null)
            throw new NCE(s"Model element ID is not provided.'")
        else if (elm.getId.length == 0)
            throw new NCE(s"Model element ID cannot be empty.'")
        else {
            val elmId = elm.getId
            
            if (elmId.toLowerCase.startsWith("nlp:"))
                throw new NCE(s"Model element '$elmId' type cannot start with 'nlp:'.")
            
            if (hasWhitespace(elmId))
                throw new NCE(s"Model element ID '$elmId' cannot have whitespaces.")
            
            if (elm.getType == null)
                throw new NCE(s"Type is not provided for model element '$elmId'.")
            else if (!ELM_TYPES.contains(elm.getType))
                throw new NCE(s"Unknown type '${elm.getType}' for model element '$elmId'.")
        }
    }
    
    /**
      * Checks whether or not given string has any whitespaces.
      * 
      * @param s String to check.
      * @return
      */
    private def hasWhitespace(s: String): Boolean =
        s.exists(_.isWhitespace)
    
    /**
      * Checks if synonym is dynamic.
      *
      * @param syn Synonym to check.
      */
    private def isDynamic(syn: String): Boolean =
        syn.startsWith("%") && syn.endsWith("%")
    
    /**
      * Makes usage holder.
      *
      * @param mdl Model.
      * @param parser Initialized macro parser.
      */
    @throws[NCE]
    private def makeModelUsage(mdl: NCModel, parser: MacroParser): NCModelUsage = {
        val elements = mutable.HashSet.empty[NCElementUsage]
        
        // Process elements.
        for (elm ← mdl.getElements.asScala) {
            val exampleSyns = nlv(elm.getSynonyms) // Take un-processed synonyms in their original form.
                .map(_.trim)
                .filter(!isDynamic(_))
                .take(3)
                .map(parser.expand(_).head)
            
            elements += NCElementUsage(
                id = elm.getId,
                group = elm.getGroup,
                `type` = elm.getType,
                description = elm.getDescription,
                synonyms = exampleSyns
            )
        }
        
        NCModelUsage(
            id = mdl.getDescriptor.getId,
            name = mdl.getDescriptor.getName,
            version = mdl.getDescriptor.getVersion,
            description = mdl.getDescription,
            docsUrl = mdl.getDocsUrl,
            vendorUrl = mdl.getVendorUrl,
            vendorContact = mdl.getVendorContact,
            vendorName = mdl.getVendorName,
            vendorEmail = mdl.getVendorEmail,
            examples = mdl.getExamples.asScala.toList,
            elements = elements.toList
        )
    }
    
    /**
      *
      * @param mdl Model.
      */
    private def checkModelConfig(mdl: NCModel): Unit = {
        def checkInt(v: Int, name: String, min: Int = 0, max: Int = Integer.MAX_VALUE): Unit = 
            if (v < min)
                throw new NCE(s"Invalid model configuration value '$name' [value=$v, min=$min]")
            else if (v > max)
                throw new NCE(s"Invalid model configuration value '$name' [value=$v, max=$min]")
    
        checkInt(mdl.getMaxUnknownWords, "maxUnknownWords")
        checkInt(mdl.getMaxFreeWords, "maxFreeWords")
        checkInt(mdl.getMaxSuspiciousWords, "maxSuspiciousWords")
        checkInt(mdl.getMinWords, "minWords", min = 1)
        checkInt(mdl.getMinNonStopwords, "minNonStopwords")
        checkInt(mdl.getMinTokens, "minTokens")
        checkInt(mdl.getMaxTokens, "maxTokens", max = 100)
        checkInt(mdl.getMaxWords, "maxWords", min = 1, max = 100)
        checkInt(mdl.getMaxGeoTokens, "maxGeoTokens")
        checkInt(mdl.getMinGeoTokens, "minGeoTokens")
        checkInt(mdl.getMaxDateTokens, "maxDateTokens")
        checkInt(mdl.getMinDateTokens, "minDateTokens")
        checkInt(mdl.getMaxNumTokens, "maxNumTokens")
        checkInt(mdl.getMinNumTokens, "minNumTokens")
        checkInt(mdl.getMaxFunctionTokens, "maxFunctionTokens")
        checkInt(mdl.getMinFunctionTokens, "minFunctionTokens")
        checkInt(mdl.getJiggleFactor, "jiggleFactor", max = 4)
    }
    
    /**
      * Converts Java collection.
      *
      * @param col Java collection.
      */
    private def nlv(col: JCollection[String]): List[String] =
        if (col == null) List.empty else col.asScala.toList
    
    /**
      * Generates trivia data.
      *
      * @param mdl Model.
      * @param parser Initialized macro parser.
      */
    private def generateTrivia(mdl: NCModel, parser: MacroParser): Map[String, Set[String]] =
        if (mdl.getTrivia == null)
            Map.empty
        else {
            val groups = mdl.getTrivia.asScala.toSet
            
            def expand(seq: Seq[String]): Seq[String] = seq.flatMap(parser.expand).map(_.trim)
            
            groups.
                map(g ⇒ (nlv(g.getInputs), nlv(g.getResponses))).
                flatMap(h ⇒
                    expand(h._1).
                        map(txt ⇒ txt.split(" ").map(_.trim).filter(_.nonEmpty).mkString(" ").toLowerCase.trim).map(p ⇒
                        p → expand(h._2).toSet
                    )
                ).toMap
        }
    
    /**
      *
      * @return
      */
    def getAllModels: List[NCModelDecorator] = {
        ensureStarted()
    
        mux.synchronized {
            models.values.toList
        }
    }
    
    /**
      *
      * @param id Model ID.
      * @return
      */
    def getModel(id: String): Option[NCModelDecorator] = {
        ensureStarted()
    
        mux.synchronized {
            models.get(id)
        }
    }
    
    /**
      * 
      * @param modelId Model ID.
      */
    def reload(modelId: String): Unit = {
        ensureStarted()
        
        mux.synchronized {
            logger.info(s"Started reloading model: $modelId")
    
            models.remove(modelId) match {
                case Some(m) ⇒
                    // Discard current model instance.
                    discardModel(m.model)
    
                    // Scan providers to reload the same model.
                    for (provider ← NCDeployManager.getProviders)
                        addNewModel(provider, modelId)
                    
                    models.get(modelId) match {
                        case Some(mdl) ⇒
                            // Invariant.
                            require(mdl.model.getDescriptor.getId == modelId)
                            
                            // Ack.
                            val tbl = NCAsciiTable("Model ID", "Name", "Ver.", "Elements", "Synonyms")
    
                            val synCnt = mdl.synonyms.values.flatMap(_.values).flatten.size
                            val newDs = mdl.model.getDescriptor
    
                            tbl += (
                                newDs.getId,
                                newDs.getName,
                                newDs.getVersion,
                                mdl.elements.keySet.size,
                                synCnt
                            )
    
                            tbl.info(logger, Some(s"Model reloaded: $modelId\n"))
                            
                        case None ⇒
                            logger.error(s"Failed to reload model: $modelId")
                    }
                    
                case None ⇒
                    logger.error(s"Failed to reload unknown model: $modelId")
            }
        }
    }
    
    /**
      *
      * @return
      */
    def getAllUsage: List[NCModelUsage] = {
        ensureStarted()
    
        mux.synchronized {
            usages.values.toList
        }
    }
}
