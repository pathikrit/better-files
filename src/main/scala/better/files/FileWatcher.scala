package better.files

import akka.actor

/**
 * An actor that can watch a file or a directory
 * Instead of directly calling the constructor of this, call file.newWatcher to create the actor
 *
 * @param file watch this file (or directory)
 * @param maxDepth In case of directories, how much depth should we watch
 */
class FileWatcher(file: File, maxDepth: Int = 0) extends actor.Actor {

  def this(file: File, recursive: Boolean) = this(file, if (recursive) Int.MaxValue else 0)

  protected[this] val watcher = new FileMonitor(file, maxDepth) {
    override def onStandardEvent(event: FileWatcher.Event, file: File) = self ! (event -> file)
    override def onException(exception: Throwable) = self ! actor.Status.Failure(exception)
  }

  protected[this] val callbacks = newMultiMap[FileWatcher.Event, FileWatcher.Callback]

  override def preStart() = watcher.start()

  override def receive = {
    case (event: FileWatcher.Event @unchecked, target: File) if callbacks contains event => callbacks(event) foreach {f => f(event -> target)}
    case FileWatcher.RegisterCallback(events, callback) => events foreach {event => callbacks.addBinding(event, callback)}
    case FileWatcher.RemoveCallback(event, callback) => callbacks.removeBinding(event, callback)
  }

  override def postStop() = watcher.interrupt()
}

object FileWatcher {
  import java.nio.file.{Path, WatchEvent}
  type Event = WatchEvent.Kind[Path]
  type FileEvent = (Event, File)
  type Callback = PartialFunction[FileEvent, Unit]

  case class RegisterCallback(events: Seq[Event], callback: Callback)
  case class RemoveCallback(event: Event, callback: Callback)
}
