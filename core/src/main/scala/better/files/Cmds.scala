package better.files

import java.nio.file.attribute.{PosixFileAttributes, PosixFilePermission, PosixFilePermissions}
import java.util.zip.{Deflater, ZipOutputStream}

import scala.collection.JavaConverters._
import scala.io.Codec

/**
  * Do file ops using a UNIX command line DSL
  */
object Cmds {
  def ~ : File =
    File.home

  def pwd: File =
    File.currentWorkingDirectory

  def cwd: File =
    pwd

  val `..`: File => File =
    _.parent

  val  `.`: File => File =
    identity

  implicit class FileDsl(file: File) {
    /**
      * Allows navigation up e.g. file / .. / ..
      *
      * @param f
      * @return
      */
    def /(f: File => File): File =
      f(file)
  }

  def cp(file1: File, file2: File): File =
    file1.copyTo(file2, overwrite = true)

  def mv(file1: File, file2: File): File =
    file1.moveTo(file2, overwrite = true)

  def rm(file: File): File =
    file.delete(swallowIOExceptions = true)

  def del(file: File): File =
    rm(file)

  def ln(file1: File, file2: File): File =
    file1.linkTo(file2)

  def ln_s(file1: File, file2: File): File =
    file1.symbolicLinkTo(file2)

  def cat(files: File*): Seq[Iterator[Byte]] =
    files.map(_.bytes)

  def ls(file: File): Files =
    file.list

  def dir(file: File): Files =
    ls(file)

  def ls_r(file: File): Files =
    file.listRecursively

  def touch(file: File): File =
    file.touch()

  def mkdir(file: File): File =
    file.createDirectory()

  def md5(file: File): String =
    file.md5

  def sha1(file: File): String =
    file.sha1

  def sha256(file: File): String =
    file.sha256

  def sha512(file: File): String =
    file.sha512

  def mkdirs(file: File): File =
    file.createDirectories()

  def chown(owner: String, file: File): File =
    file.setOwner(owner)

  def chgrp(group: String, file: File): File =
    file.setGroup(group)

  /**
    * Update permission of this file
    *
    * @param permissions Must be 9 character POSIX permission representation e.g. "rwxr-x---"
    * @param file
    * @return file
    */
  def chmod(permissions: String, file: File): File =
    file.setPermissions(PosixFilePermissions.fromString(permissions).asScala.toSet)

  def chmod_+(permission: PosixFilePermission, file: File): File =
    file.addPermission(permission)

  def chmod_-(permission: PosixFilePermission, file: File): File =
    file.removePermission(permission)

  def stat(file: File): PosixFileAttributes =
    file.posixAttributes

  def unzip(zipFile: File)(destination: File)(implicit codec: Codec): File =
    zipFile.unzipTo(destination)(codec)

  def zip(files: File*)(destination: File, compressionLevel: Int = Deflater.DEFAULT_COMPRESSION)(implicit codec: Codec): File = {
    for {
      output <- new ZipOutputStream(destination.newOutputStream, codec).withCompressionLevel(compressionLevel).autoClosed
      input <- files
      file <- input.walk()
      name = input.parent relativize file
    } output.add(file, name.toString)
    destination
  }
}
