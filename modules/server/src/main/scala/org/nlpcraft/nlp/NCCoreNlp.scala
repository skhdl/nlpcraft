package org.nlpcraft.nlp

import org.nlpcraft.NCLifecycle

object NCCoreNlp extends NCLifecycle("CORE NLP") {
    def stemmatizeSeq(strings: Seq[String]): Seq[String] = ???

    def lemmatize(s: String): String = ???

    def stemmatize(s: String): String = ???

}
