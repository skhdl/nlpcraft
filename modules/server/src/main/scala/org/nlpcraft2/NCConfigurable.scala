package org.nlpcraft2

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Mixin for configuration factory (defined in standard 'nlpcraft.conf').
 */
trait NCConfigurable {
    // Configuration factory loading 'nlpcraft.conf'.
    protected val hocon: Config = ConfigFactory.load()

    /**
     * Calling this function will touch the object and trigger
     * the eager evaluation.
     */
    def check(): Unit = ()
}
