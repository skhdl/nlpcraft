package org.nlpcraft.nlp.opennlp

import java.io.BufferedInputStream

import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.stemmer.{PorterStemmer, Stemmer}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import org.nlpcraft.{G, NCLifecycle}
import resource.managed

import scala.collection.Seq

/**
  * OpenNLP manager.
  */
object NCNlpManager extends NCLifecycle("OpenNLP manager") {
    @volatile private var tokenizer: Tokenizer = _
    @volatile private var tagger: POSTagger = _
    @volatile private var lemmatizer: DictionaryLemmatizer = _
    @volatile private var nameFinder: NameFinderME = _
    @volatile private var stemmer: Stemmer = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        tokenizer =
            managed(new BufferedInputStream(G.getStream("opennlp/en-token.bin"))) acquireAndGet { in ⇒
                new TokenizerME(new TokenizerModel(in))
            }

        tagger =
            managed(new BufferedInputStream(G.getStream("opennlp/en-pos-maxent.bin"))) acquireAndGet { in ⇒
                new POSTaggerME(new POSModel(in))
            }

        nameFinder =
            managed(new BufferedInputStream(G.getStream("opennlp/en-ner-location.bin"))) acquireAndGet { in ⇒
                new NameFinderME(new TokenNameFinderModel(in))
            }

        lemmatizer =
            managed(new BufferedInputStream(G.getStream("opennlp/en-lemmatizer.dict"))) acquireAndGet { in ⇒
                new DictionaryLemmatizer(in)
            }

        stemmer = new PorterStemmer

        super.start()
    }

    /**
      * Parses given sentence.
      *
      * @param sen Sentence text.
      * @return Parsed tokens.
      */
    def parse(sen: String): Seq[NCNlpWord] = {
        ensureStarted()

        // Can be optimized.
        val (spans, words, poses, lemmas) =
            this.synchronized {
                val spans = tokenizer.tokenizePos(sen)
                val words = spans.map(_.getCoveredText(sen).toString)
                val poses = tagger.tag(words)

                require(spans.length == poses.length)

                val lemmas = lemmatizer.lemmatize(words, poses)

                require(spans.length == lemmas.length)

                (spans, words, poses, lemmas)
            }

        spans.zip(words).zip(poses).zip(lemmas).map { case (((span, word), pos), lemma) ⇒
            val normalWord = word.toLowerCase

            NCNlpWord(
                word = word,
                normalWord = normalWord,
                // "0" is flag that lemma cannot be obtained for some reasons.
                lemma = if (lemma == "O") None else Some(lemma),
                stem = stemmer.stem(normalWord).toString,
                pos = pos,
                start = span.getStart,
                end = span.getEnd,
                length = span.length
            )
        }
    }

    /**
      * Tokenizes given sentence.
      *
      * @param sen Sentence text.
      * @return Tokens.
      */
    def tokenize(sen: String): Seq[String] = {
        ensureStarted()

        this.synchronized { tokenizer.tokenize(sen) }
    }

    /**
      * Stems given word (input text tokenized before).
      *
      * @param sen Sentence text.
      * @return Sentence with stemmed words.
      */
    def stemSentence(sen: String): String = {
        ensureStarted()

        val seq = this.synchronized {
            tokenizer.tokenizePos(sen).map(span ⇒ (span, stemmer.stem(span.getCoveredText(sen).toString)))
        }

        seq.zipWithIndex.map { case ((span, stem), idx) ⇒
            idx match {
                case 0 ⇒ stem
                // Suppose there aren't multiple spaces.
                case _ ⇒ if (seq(idx - 1)._1.getEnd <  span.getStart) s" $stem" else stem
            }
        }.mkString("")
    }

    /**
      * Stems given word (input text is not tokenized before).
      *
      * @param word Word.
      * @return Stem.
      */
    def stemWord(word: String): String = {
        ensureStarted()

        this.synchronized { stemmer.stem(word).toString }
    }

    /**
      * Gets indexes for words which detected as location.
      * Note that if can detect location taking into accounts their case
      * (`Moscow` detected, `MOSCOW` or `moscow` is not detected)
      *
      * @param words Words
      * @return Indexes list.
      */
    def findLocations(words: Seq[String]): Seq[Int] = {
        ensureStarted()

        this.
            synchronized { nameFinder.find(words.toArray) }.
            flatMap(p ⇒ Range.inclusive(p.getStart, p.getEnd - 1))
    }
}
