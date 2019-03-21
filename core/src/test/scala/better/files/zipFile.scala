package better.files

import java.util.zip.Deflater

object zipFile extends App{

  // Have to add a mimetype file as the first entry in a ZIP file and it must be stored uncompressed (“Stored” mode).
  val file: File = "mimetype".toFile
  file.overwrite("application/iirds+zip")
  assert(file.contentAsString == "application/iirds+zip")

  val zipFile: File = file.zipTo("test.iirds".toFile, compressionLevel = Deflater.NO_COMPRESSION)

}
