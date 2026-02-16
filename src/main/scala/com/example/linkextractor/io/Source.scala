package com.example.linkextractor.io

import com.example.linkextractor.utils.Logging

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import scala.io.{Source => ScalaSource}
import scala.util.control.NonFatal

/**
 * URL source abstraction.
 *
 * Implementations provide a lazy iterator over normalized URLs and support
 * explicit cleanup. The iterator is one-pass and intended to be consumed once.
 */
trait Source {

  /**
   * Lazy URL stream.
   *
   * @return
   *   One-pass iterator of normalized URL strings.
   */
  def stream: Iterator[String]

  /**
   * Releases source resources.
   */
  def close(): Unit
}

object Source extends Logging {

  /**
   * Creates a source from direct URL inputs.
   *
   * @param inputs
   *   Raw input values.
   * @return
   *   In-memory source yielding normalized URLs.
   */
  def fromInputs(inputs: Iterable[String]): Source =
    new InMemorySource(normalize(inputs.iterator))

  /**
   * Creates a source from a newline-delimited URL file.
   *
   * @param path
   *   Input file path.
   * @return
   *   File-backed source yielding normalized URLs.
   */
  def fromFile(path: String): Source =
    fromFile(Paths.get(path))

  /**
   * Creates a source from a newline-delimited URL file.
   *
   * @param path
   *   Input file path.
   * @return
   *   File-backed source yielding normalized URLs.
   */
  def fromFile(path: Path): Source =
    fromFile(path = path, charset = StandardCharsets.UTF_8)

  /**
   * Creates a source from a newline-delimited URL file.
   *
   * @param path
   *   Input file path.
   * @param charset
   *   File charset.
   * @return
   *   File-backed source yielding normalized URLs.
   */
  def fromFile(
    path: Path,
    charset: Charset
  ): Source = {
    val underlying = ScalaSource.fromFile(path.toFile, charset.name())
    new FileSource(
      stream = normalize(underlying.getLines()),
      closeFn = () => closeQuietly(underlying, context = s"file [$path]")
    )
  }

  /**
   * Normalizes source values by trimming and dropping blank/comment lines.
   *
   * @param values
   *   Raw source values.
   * @return
   *   Normalized URL iterator.
   */
  private def normalize(values: Iterator[String]): Iterator[String] =
    values
      .map(_.trim)
      .filter(value => value.nonEmpty && !value.startsWith("#"))

  /**
   * Closes a Scala source and suppresses close-time failures.
   *
   * @param underlying
   *   Underlying Scala source.
   * @param context
   *   Logging context.
   */
  private def closeQuietly(underlying: ScalaSource, context: String): Unit =
    try underlying.close()
    catch {
      case NonFatal(error) =>
        logger.warn(s"Failed to close source for $context: ${error.getMessage}")
    }

  /**
   * In-memory source implementation.
   *
   * @param stream
   *   Pre-normalized URL iterator.
   */
  private final class InMemorySource(
    override val stream: Iterator[String]
  ) extends Source {

    /** In-memory source has no closeable resources. */
    override def close(): Unit = ()
  }

  /**
   * File-backed source implementation.
   *
   * @param stream
   *   Pre-normalized URL iterator.
   * @param closeFn
   *   Source close function.
   */
  private final class FileSource(
    override val stream: Iterator[String],
    closeFn: () => Unit
  ) extends Source {

    /** Closes underlying file resources. */
    override def close(): Unit = closeFn()
  }
}
