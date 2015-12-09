package better.files

import java.nio.file._

/**
 * A thread that can monitor a file (or a directory)
 * This class's onDelete, onCreate, onModify etc are available to be overridden
 *
 * @param root
 * @param maxDepth
 */
abstract class FileMonitor(root: File, maxDepth: Int) extends Thread { //TODO: Maybe this should be File.Monitor?

  def this(root: File, recursive: Boolean = true) = this(root, if (recursive) Int.MaxValue else 0)

  setDaemon(true)
  setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler {
    override def uncaughtException(thread: Thread, exception: Throwable) = onException(exception)
  })

  protected[this] val service = root.newWatchService

  override def run() = Iterator.continually(service.take()) foreach process

  override def interrupt() = {
    service.close()
    super.interrupt()
  }

  override def start() = {
    watch(root, maxDepth)
    super.start()
  }

  protected[this] def process(key: WatchKey) = {
    def reactTo(target: File) = root.isDirectory || (root isSamePathAs target) // if watching non-directory, don't react to siblings

    val path = key.watchable().asInstanceOf[Path]

    import scala.collection.JavaConversions._
    key.pollEvents() foreach {
      case event: WatchEvent[Path] @unchecked =>
        val target: File = path resolve event.context()
        if (reactTo(target)) {
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            val depth = (root relativize target).getNameCount
            watch(target, (maxDepth - depth) max 0) // auto-watch new files in a directory
          }
          repeat(event.count())(dispatch(event.kind(), target))
        }
      case event => if (reactTo(path)) onUnknownEvent(event, path)
    }
    key.reset()
  }

  protected[this] def watch(file: File, depth: Int): Unit = if (file.isDirectory) {
    for {
      f <- file.walk(depth) if f.isDirectory && f.exists
    } f.register(service)
  } else if (file.exists) file.parent.register(service)   // There is no way to watch a regular file; so watch its parent instead

  /**
   * Dispatch a StandardWatchEventKind to an appropriate callback
   * Override this if you don't want to manually handle onDelete/onCreate/onModify separately
   *
   * @param eventType
   * @param file
   */
  def dispatch(eventType: WatchEvent.Kind[Path], file: File): Unit = eventType match {
    case StandardWatchEventKinds.ENTRY_CREATE => onCreate(file)
    case StandardWatchEventKinds.ENTRY_MODIFY => onModify(file)
    case StandardWatchEventKinds.ENTRY_DELETE => onDelete(file)
  }

  def onCreate(file: File): Unit = {}

  def onModify(file: File): Unit = {}

  def onDelete(file: File): Unit = {}

  def onUnknownEvent(event: WatchEvent[_], root: File): Unit = {}

  def onException(exception: Throwable): Unit = {}
}
