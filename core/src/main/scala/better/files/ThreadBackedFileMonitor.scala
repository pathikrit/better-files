package better.files

import java.nio.file._

import scala.util.control.NonFatal

/**
  * A thread based implementation of the FileMonitor
  *
  * @param root
  * @param maxDepth
  */
abstract class ThreadBackedFileMonitor(val root: File, maxDepth: Int) extends File.Monitor {
  protected[this] val service = root.newWatchService

  private[this] val thread = new Thread {
    override def run() = Iterator.continually(service.take()) foreach process
  }
  thread.setDaemon(true)
  thread.setUncaughtExceptionHandler((thread, exception) => onException(exception))

  def this(root: File, recursive: Boolean = true) = this(root, if (recursive) Int.MaxValue else 0)

  protected[this] def process(key: WatchKey) = {
    def reactTo(target: File) = root.isDirectory || (root isSamePathAs target) // if watching non-directory, don't react to siblings

    val path = key.watchable().asInstanceOf[Path]

    import scala.collection.JavaConverters._
    key.pollEvents().asScala foreach {
      case event: WatchEvent[Path] @unchecked =>
        val target: File = path resolve event.context()
        if (reactTo(target)) {
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            val depth = (root relativize target).getNameCount
            watch(target, (maxDepth - depth) max 0) // auto-watch new files in a directory
          }
          repeat(event.count())(onEvent(event.kind(), target))
        }
      case event => if (reactTo(path)) onUnknownEvent(event)
    }
    key.reset()
  }

  protected[this] def watch(file: File, depth: Int): Unit = {
    val toWatch: Files = if (file.isDirectory) {
      file.walk(depth).filter(f => f.isDirectory && f.exists)
    } else {
      when(file.exists)(file.parent).iterator  // There is no way to watch a regular file; so watch its parent instead
    }

    toWatch.foreach { f =>
      try { f.register(service) } catch { case NonFatal(e) => onException(e) }
    }
  }

  override def start() = {
    watch(root, maxDepth)
    thread.start()
  }

  override def stop() = {
    service.close()
    thread.interrupt()
  }

  // Although we declare this class as abstract, we give empty implementations here so users can choose to implement a subset of these
  override def onCreate(file: File) = {}
  override def onModify(file: File) = {}
  override def onDelete(file: File) = {}
  override def onUnknownEvent(event: WatchEvent[_]) = {}
  override def onException(exception: Throwable) = {}
}
