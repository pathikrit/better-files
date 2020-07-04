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

import better.files.File
import java.io.{File => JFile, FileInputStream, InputStream}
import org.apache.commons.codec.digest.{DigestUtils => Apache}

/**
  * Simple Wrapper around org.apache.commons.codec.digest.DigestUtils
  */
object DigestUtils {
  def md5(data: Array[Byte]): Array[Byte] = Apache.md5(data)
  def md5(data: InputStream): Array[Byte] = Apache.md5(data)
  def md5(data: String): Array[Byte]      = Apache.md5(data)
  def md5(file: File): Array[Byte]        = md5(file.toJava)
  def md5(file: JFile): Array[Byte]       = Resource.using(new FileInputStream(file)) { md5 }

  def md5Hex(data: Array[Byte]): String = Apache.md5Hex(data)
  def md5Hex(data: InputStream): String = Apache.md5Hex(data)
  def md5Hex(data: String): String      = Apache.md5Hex(data)
  def md5Hex(file: File): String        = md5Hex(file.toJava)
  def md5Hex(file: JFile): String       = Resource.using(new FileInputStream(file)) { md5Hex }

  def sha1(data: Array[Byte]): Array[Byte] = Apache.sha1(data)
  def sha1(data: InputStream): Array[Byte] = Apache.sha1(data)
  def sha1(data: String): Array[Byte]      = Apache.sha1(data)
  def sha1(file: File): Array[Byte]        = sha1(file.toJava)
  def sha1(file: JFile): Array[Byte]       = Resource.using(new FileInputStream(file)) { sha1 }

  def sha1Hex(data: Array[Byte]): String = Apache.sha1Hex(data)
  def sha1Hex(data: InputStream): String = Apache.sha1Hex(data)
  def sha1Hex(data: String): String      = Apache.sha1Hex(data)
  def sha1Hex(file: File): String        = sha1Hex(file.toJava)
  def sha1Hex(file: JFile): String       = Resource.using(new FileInputStream(file)) { sha1Hex }

  def sha256(data: Array[Byte]): Array[Byte] = Apache.sha256(data)
  def sha256(data: InputStream): Array[Byte] = Apache.sha256(data)
  def sha256(data: String): Array[Byte]      = Apache.sha256(data)
  def sha256(file: File): Array[Byte]        = sha256(file.toJava)
  def sha256(file: JFile): Array[Byte]       = Resource.using(new FileInputStream(file)) { sha256 }

  def sha256Hex(data: Array[Byte]): String = Apache.sha256Hex(data)
  def sha256Hex(data: InputStream): String = Apache.sha256Hex(data)
  def sha256Hex(data: String): String      = Apache.sha256Hex(data)
  def sha256Hex(file: File): String        = sha256Hex(file.toJava)
  def sha256Hex(file: JFile): String       = Resource.using(new FileInputStream(file)) { sha256Hex }
}
