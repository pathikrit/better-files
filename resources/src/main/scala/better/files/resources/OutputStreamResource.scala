package better.files.resources

import java.io._
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import org.apache.commons.compress.archivers.tar.{TarArchiveOutputStream, TarArchiveEntry}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.util.zip.{Deflater, GZIPOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.util.jar.{JarEntry, JarOutputStream}

object OutputStreamResource {
  def wrap(
      os: OutputStream,
      fileName: String = "",
      autoCompress: Boolean = true,
      compressionLevel: Int = Deflater.BEST_SPEED,
      buffered: Boolean = true,
      internalArchiveFileName: Option[String] = None
  ): OutputStreamResource = {
    OutputStreamResource(
      SingleUseResource(os),
      fileName = fileName,
      autoCompress = autoCompress,
      compressionLevel = compressionLevel,
      buffered = buffered,
      internalArchiveFileName = internalArchiveFileName
    )
  }

  /** Simple wrapper to allow specifying compressiong level in the constructor */
  final private class ConfigurableGzipOutputStream(os: OutputStream, level: Int) extends GZIPOutputStream(os) {
    `def`.setLevel(level)
  }
}

final case class OutputStreamResource(
    resource: Resource[OutputStream],
    fileName: String = "",
    autoCompress: Boolean = true,
    compressionLevel: Int = Deflater.BEST_SPEED,
    buffered: Boolean = true,
    internalArchiveFileName: Option[String] = None
) extends Resource[OutputStream] {
  def isUsable: Boolean   = resource.isUsable
  def isMultiUse: Boolean = resource.isMultiUse

  def use[T](f: OutputStream => T): T = filteredResource(bufferedFilter(resource)).use { os: OutputStream => f(os) }

  def writer(): Resource[Writer] = flatMap { is => Resource(new OutputStreamWriter(is)) }

  def writer(encoding: String): Resource[Writer] = {
    flatMap { os: OutputStream =>
      val updatedEncoding: String = if (encoding === UTF_8_BOM.name || UTF_8_BOM.aliases().contains(encoding)) {
        // Write the UTF-8 BOM
        UTF_8_BOM.writeBOM(os)

        // Switch to the normal UTF_8 Charset (even though UTF_8_BOM should work the same)
        "UTF-8"
      } else {
        encoding
      }

      Resource(new OutputStreamWriter(os, updatedEncoding))
    }
  }

  def writer(cs: Charset): Resource[Writer] = {
    flatMap { os: OutputStream =>
      val updatedCS: Charset = if (cs eq UTF_8_BOM) {
        // Write the UTF-8 BOM
        UTF_8_BOM.writeBOM(os)

        // Switch to the normal UTF_8 Charset (even though UTF_8_BOM should work the same)
        UTF_8
      } else {
        cs
      }

      Resource(new OutputStreamWriter(os, updatedCS))
    }
  }

  def bufferedWriter(): Resource[BufferedWriter]                 = writer() flatMap { r => Resource(new BufferedWriter(r)) }
  def bufferedWriter(encoding: String): Resource[BufferedWriter] = writer(encoding) flatMap { r => Resource(new BufferedWriter(r)) }
  def bufferedWriter(cs: Charset): Resource[BufferedWriter]      = writer(cs) flatMap { r => Resource(new BufferedWriter(r)) }

  def dataOutput(): Resource[DataOutput] = flatMap { os => Resource(new DataOutputStream(os)) }

  private def filteredResource(resource: Resource[OutputStream]): Resource[OutputStream] = {
    import Resource._

    val lowerFileName: String = fileName.toLowerCase

    if (!autoCompress) resource
    else if (lowerFileName.endsWith(".tar.gz")) gzip(tar(resource, ".tar.gz"))
    else if (lowerFileName.endsWith(".tgz")) gzip(tar(resource, ".tgz"))
    else if (lowerFileName.endsWith(".tbz2")) bzip2(tar(resource, ".tbz2"))
    else if (lowerFileName.endsWith(".tbz")) bzip2(tar(resource, ".tbz"))
    else if (lowerFileName.endsWith(".tar")) tar(resource, ".tar")
    else if (lowerFileName.endsWith(".gz")) gzip(resource)
    else if (lowerFileName.endsWith(".bzip2")) bzip2(resource)
    else if (lowerFileName.endsWith(".bz2")) bzip2(resource)
    else if (lowerFileName.endsWith(".bz")) bzip2(resource)
    else if (lowerFileName.endsWith(".snappy")) snappy(resource)
    else if (lowerFileName.endsWith(".xz")) xz(resource)
    else if (lowerFileName.endsWith(".zip")) zip(resource, ".zip")
    else if (lowerFileName.endsWith(".jar")) jar(resource, ".jar")
    else resource
  }

  private def gzip(r: Resource[OutputStream]): Resource[OutputStream] =
    r.flatMap { new OutputStreamResource.ConfigurableGzipOutputStream(_, compressionLevel) }

  private def snappy(r: Resource[OutputStream]): Resource[OutputStream] =
    r.flatMap { Snappy.newOutputStream(_) }

  private def bzip2(r: Resource[OutputStream]): Resource[OutputStream] =
    r.flatMap { new BZip2CompressorOutputStream(_) }

  private def xz(r: Resource[OutputStream]): Resource[OutputStream] =
    r.flatMap { new XZCompressorOutputStream(_) }

  private def tar(r: Resource[OutputStream], extension: String): Resource[OutputStream] =
    r.flatMap { os: OutputStream =>
      val tos = new TarArchiveOutputStream(os)
      // Add an entry with the extension stripped off
      val entryName: String = internalArchiveFileName.getOrElse(fileName.substring(0, fileName.length - extension.length))
      tos.putArchiveEntry(new TarArchiveEntry(entryName))
      tos
    }

  private def zip(r: Resource[OutputStream], extension: String): Resource[OutputStream] =
    r.flatMap { os: OutputStream =>
      val zos = new ZipOutputStream(os)
      zos.setLevel(compressionLevel)
      // Add an entry with the extension stripped off
      val entryName: String = internalArchiveFileName.getOrElse(fileName.substring(0, fileName.length - extension.length))
      zos.putNextEntry(new ZipEntry(entryName))
      zos
    }

  private def jar(r: Resource[OutputStream], extension: String): Resource[OutputStream] =
    r.flatMap { os: OutputStream =>
      val zos = new JarOutputStream(os)
      zos.setLevel(compressionLevel)
      // Add an entry with the extension stripped off
      val entryName: String = internalArchiveFileName.getOrElse(fileName.substring(0, fileName.length - extension.length))
      zos.putNextEntry(new JarEntry(entryName))
      zos
    }

  private def bufferedFilter(resource: Resource[OutputStream]): Resource[OutputStream] = {
    if (buffered) resource.flatMap { new BufferedOutputStream(_) }
    else resource
  }
}
