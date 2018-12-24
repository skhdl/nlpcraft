package org.nlpcraft.nlp.opennlp

/**
  * Word information holder.
  *
  * @param word Original word.
  * @param normalWord Normalized word (lower case representation)
  * @param lemma Lemma. Optional.
  * @param stem Stem.
  * @param pos POS.
  * @param start From index.
  * @param end To index.
  * @param length Length.
  */
case class NCNlpWord(
    word: String,
    normalWord: String,
    lemma: Option[String],
    stem: String,
    pos: String,
    start: Int,
    end: Int,
    length: Int
)
