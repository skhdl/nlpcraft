package org.nlpcraft2

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Mixin for configuration factory (defined in standard 'application.conf').
 */
trait NCConfigurable {
    // Configuration factory loading 'application.conf'.
    protected val hocon: Config = ConfigFactory.load()

    /**
     * Calling this function will touch the object and trigger
     * the eager evaluation.
     */
    def check(): Unit = ()
}
