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

import java.util.{Timer, TimerTask}
import scala.ref.WeakReference
import scala.util.Try
import com.typesafe.scalalogging.{LazyLogging, Logger}

object ReloadableResource extends LazyLogging {
  private val timer: Timer = new Timer("ReloadableResource Check", true /* isDaemon */ )

  private class ResourceCheckTimerTask[T](resource: ReloadableResource[T], logger: Logger) extends TimerTask {
    private[this] val ref: WeakReference[ReloadableResource[T]] = WeakReference(resource)

    /** This should be the lastModified date of the currently loaded resource */
    private[this] var lastModified: Long = lookupLastModified(resource).getOrElse { -1 }

    def run(): Unit = {
      ref.get match {
        case None    => cancel() // The reference has been cleared so cancel the task
        case Some(r) => lookupLastModified(r).foreach { doReload(r, _) }
      }
    }

    private def lookupLastModified(r: ReloadableResource[T]): Option[Long] = Try { r.lookupLastModified() }.toOption

    private def doReload(r: ReloadableResource[T], currentLastModified: Long): Unit = {
      if (currentLastModified == lastModified) return

      logger.info("Detected Updated Resource, Reloading...")
      if (r.reload()) {
        // We only update the lastModified if the resource was successfully reloaded.
        // This still isn't very fullproof and should really be modified so that reload
        // returns an Option[Long] which is the lastModified of the reloaded resource.
        lastModified = currentLastModified
        logger.info("Reload Successful.")
      } else {
        logger.info("Reload Failed.")
      }
    }
  }

}

abstract class ReloadableResource[T] extends LazyLogging {
  import ReloadableResource.ResourceCheckTimerTask

  /** Load the resource from it's primary source */
  protected def loadFromPrimary(): Option[T]

  /** Load the resource from it's backup source (if any) */
  protected def loadFromBackup(): Option[T]

  /** A backup backup resource that will be used if the files and backup cannot be loaded */
  protected def defaultResource: Option[T]

  /** The Last Modified time of the resource (can be set to System.currentTimeMillis to always reload) */
  protected def lookupLastModified(): Long

  @volatile private[this] var _current: T = null.asInstanceOf[T]

  private lazy val init: Unit = {
    _current = loadResource()
  }

  /** Get the current version of the resource */
  final def apply(): T = {
    init
    _current
  }

  /**
    * Attempt to reload the current resource.  If there is a problem the existing version will be left in place
    *
    * Returns true if the resource was successfully updated
    *
    * TODO: This should probably return an Option[Long] which is the last modified time of the reloaded resource
    */
  final def reload(): Boolean =
    tryLoadResource(tryBackup = false) match {
      case None => false
      case Some(resource) =>
        _current = resource
        true
    }

  /**
    * Clear the reference to the current version of the resource.
    *
    * NOTE: Calling apply() after this will return null
    */
  final def clear(): Unit = {
    _current = null.asInstanceOf[T]
  }

  /** Directly load the resource and return the result.  Doesn't touch the current resource in this class. */
  final def loadResource(): T =
    (tryLoadResource(tryBackup = true) orElse defaultResource).getOrElse { throw new Exception("Unable to load resource") }

  private[this] var timerTask: ResourceCheckTimerTask[T] = null

  final def isAutoUpdateCheckEnabled: Boolean = null != timerTask

  /**
    * Enable checking and automatic reload of the resource if the external file is updated
    */
  final def enableAutoUpdateCheck(delaySeconds: Int = 300, periodSeconds: Int = 300): Unit = {
    require(null == timerTask, "TimerTask already enabled!")
    timerTask = new ResourceCheckTimerTask(this, logger)
    ReloadableResource.timer.schedule(timerTask, delaySeconds.toLong * 1000L, periodSeconds.toLong * 1000L)
  }

  /** Disable the auto update checks */
  final def disableAutoUpdateCheck(): Unit = {
    Option(timerTask).foreach { _.cancel() }
    timerTask = null
  }

  private def tryLoadResource(tryBackup: Boolean): Option[T] = {

    try {
      val (millis, result): (Long, Option[T]) = Util.time { loadFromPrimary() }
      if (result.isDefined) {
        logger.info(s"Loaded resource from primary source ($millis ms)")
        return result
      }
    } catch {
      case ex: Exception => logger.error("Exception Loading Resource from primary source", ex)
    }

    if (!tryBackup) return None

    try {
      val (millis, result): (Long, Option[T]) = Util.time { loadFromBackup() }
      if (result.isDefined) {
        logger.info(s"Loaded resource from backup source ($millis ms)")
        return result
      }
    } catch {
      case ex: Exception => logger.error("Exception Loading Resource from backup source", ex)
    }

    None
  }

  override def finalize(): Unit = {
    disableAutoUpdateCheck()
  }
}
