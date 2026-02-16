package com.example.linkextractor.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Shared logging support.
 *
 * Classes and objects can extend this trait to access a standard SLF4J logger
 * bound to `getClass`.
 */
trait Logging {

  /**
   * Lazily initialized SLF4J logger bound to runtime class.
   *
   * @return
   *   Logger instance for this class/object.
   */
  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
