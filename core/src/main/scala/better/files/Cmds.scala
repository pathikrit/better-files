package better.files

import java.nio.file.attribute.{PosixFilePermissions, PosixFilePermission}
import java.util.zip.ZipOutputStream

import scala.collection.JavaConversions._

/**
 * Do file ops using a UNIX command line DSL
 */
object Cmds {
  def cp(file1: File, file2: File): File = file1.copyTo(file2, overwrite = true)

  def mv(file1: File, file2: File): File = file1.moveTo(file2, overwrite = true)

  def rm(file: File): File = file.delete(ignoreIOExceptions = true)
  def del(file: File): File = rm(file)

  def ln(file1: File, file2: File): File = file1 linkTo file2

  def ln_s(file1: File, file2: File): File = file1 symbolicLinkTo file2

  def cat(files: File*): Seq[Iterator[Byte]] = files.map(_.bytes)

  def ls(file: File): Files = file.list
  def dir(file: File): Files = ls(file)

  def ls_r(file: File): Files = file.listRecursively

  def touch(file: File): File = file.touch()

  def mkdir(file: File): File = file.createDirectory()

  def md5(file: File): String = file.md5

  def mkdirs(file: File): File = file.createDirectories()

  def chown(owner: String, file: File): File = file.setOwner(owner)

  def chgrp(group: String, file: File): File = file.setGroup(group)

  /**
   * Update permission of this file
   *
   * @param permissions Must be 9 character POSIX permission representation e.g. "rwxr-x---"
   * @param file
   * @return file
   */
  def chmod(permissions: String, file: File) = file.setPermissions(PosixFilePermissions.fromString(permissions).toSet)

  def chmod_+(permission: PosixFilePermission, file: File): File = file.addPermission(permission)

  def chmod_-(permission: PosixFilePermission, file: File): File = file.removePermission(permission)

  def unzip(zipFile: File)(destination: File): File = zipFile unzipTo destination

  def zip(files: File*)(destination: File): File = returning(destination) {
    for {
      output <- new ZipOutputStream(destination.newOutputStream).autoClosed
      input <- files
      file <- input.walk()
      name = input.parent relativize file
    } output.add(file, name.toString)
  }
}
