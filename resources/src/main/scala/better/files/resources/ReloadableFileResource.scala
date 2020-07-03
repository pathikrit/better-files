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

import java.io.{File, InputStream}

abstract class ReloadableFileResource[T] extends ReloadableResource[T] {

  /** Files to check (will choose the one with the newest timestamp) */
  protected def resourceFiles: Seq[File]

  /** If the files don't exist or fail this is a backup source that should be on the classpath */
  protected def backupResourcePath: Option[String]

  /** A backup backup resource that will be used if the files and backup cannot be loaded */
  protected def defaultResource: Option[T]

  /** Load the resource given the input stream */
  protected def loadFromInputStream(inputStream: InputStream): T

  private def sortedFilesToTry: Seq[File] = resourceFiles.filter { f => f.isFile && f.canRead }.sortBy { _.lastModified }.reverse

  protected def loadFromPrimary(): Option[T] = {
    sortedFilesToTry.foreach { file =>
      try {
        val result: Option[T] = Some(InputStreamResource.forFileOrResource(file).use { loadFromInputStream })
        logger.info("Loaded resource from: " + file.getAbsolutePath)
        return result
      } catch {
        case ex: Exception => logger.error("Exception Loading Resource from " + file.getAbsolutePath, ex)
      }
    }

    None
  }

  protected def loadFromBackup(): Option[T] =
    backupResourcePath.map { path: String => InputStreamResource.forFileOrResource(new File(path)).use { loadFromInputStream } }

  protected def lookupLastModified(): Long = sortedFilesToTry.headOption.map { _.lastModified }.getOrElse(0)
}
