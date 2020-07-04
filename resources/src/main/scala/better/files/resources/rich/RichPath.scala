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
package better.files.resources.rich

import java.nio.file.Path

final class RichPath(val self: Path) extends AnyVal {

  /** If this path starts with the passed in path then strip it */
  def stripPrefix(path: Path): Path = {
    if (self.startsWith(path)) self.subpath(path.getNameCount, self.getNameCount)
    else self
  }

  /** If this path ends with the passed in path then strip it */
  def stripSuffix(path: Path): Path = {
    if (self.endsWith(path)) self.subpath(0, self.getNameCount - path.getNameCount)
    else self
  }
}
