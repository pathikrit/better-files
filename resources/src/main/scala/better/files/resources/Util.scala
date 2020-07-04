/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package better.files.resources

import java.util.Date
import com.typesafe.scalalogging.{LazyLogging, Logger}

object Util extends LazyLogging {
  @inline def time(f: => Unit): Long = timeOnly(f)

  @inline def timeOnly(f: => Unit): Long = {
    val start: Long = System.currentTimeMillis
    f
    val end: Long   = System.currentTimeMillis
    val total: Long = end - start
    total
  }

  @inline def time[T](f: => T): Tuple2[Long, T] = {
    val start: Long = System.currentTimeMillis
    val result: T   = f
    val end: Long   = System.currentTimeMillis
    val total: Long = end - start
    (total, result)
  }

  @inline def statusMsg[T](msg: String, logger: Logger = logger)(f: => T): T = {
    logger.info(msg + "... ")
    val (total, result) = time(f)
    logger.info(msg + "... Done.  " + total + "ms")
    result
  }

  @inline def benchmark[T](name: String, logger: Logger = logger)(f: => T): T = {
    val (total, result) = time(f)
    logger.info("[BENCHMARK] " + name + ": " + total + "ms")
    result
  }

  def logAppStats[T](logger: Logger = logger)(f: => T): T = appStatsImpl(logger.info(_))(f)

  def printAppStats[T](f: => T): T = appStatsImpl(println)(f)

  private def appStatsImpl[T](out: String => Unit)(f: => T): T = {
    val start: Long         = System.currentTimeMillis
    val res: T              = f
    val end: Long           = System.currentTimeMillis
    val totalTimeSecs: Long = (end - start) / 1000

    out("Started at: " + new Date(start))
    out("  Ended at: " + new Date(end))
    out(
      "Total Time: " + totalTimeSecs + " seconds (" + ((totalTimeSecs / 60d * 100).toInt / 100d) + " minutes) (" + ((totalTimeSecs / 3600d * 100).toInt / 100d) + " hours)"
    )

    res
  }
}
