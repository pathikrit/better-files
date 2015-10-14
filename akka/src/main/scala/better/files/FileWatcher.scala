package better.files

import akka.actor
import better.files.File

/**
 * An actor that can watch a file or a directory
 * Instead of directly calling the constructor of this, call file.newWatcher to create the actor
 *
 * @param file watch this file (or directory)
 * @param maxDepth In case of directories, how much depth should we watch
 */
class FileWatcher(file: File, maxDepth: Int) extends FileMonitor(file, maxDepth) with actor.Actor {
  import FileWatcher._
  protected[this] val callbacks = newMultiMap[Event, Callback]

  def this(file: File, recursive: Boolean = true) = this(file, if (recursive) Int.MaxValue else 0)

  override def dispatch(event: Event, file: File) = self ! Message.NewEvent(event, file)
  override def onException(exception: Throwable) = self ! actor.Status.Failure(exception)

  override def preStart() = super.start()
  override def postStop() = super.interrupt()

  override def receive = {
    case Message.NewEvent(event, target) if callbacks contains event => callbacks(event) foreach {f => f(event -> target)}
    case Message.RegisterCallback(events, callback) => events foreach {event => callbacks.addBinding(event, callback)}
    case Message.RemoveCallback(event, callback) => callbacks.removeBinding(event, callback)
  }
}

object FileWatcher {
  import java.nio.file.{Path, WatchEvent}
  type Event = WatchEvent.Kind[Path]
  type Callback = PartialFunction[(Event, File), Unit]

  sealed trait Message
  object Message {
    case class NewEvent(event: Event, file: File) extends Message
    case class RegisterCallback(events: Seq[Event], callback: Callback) extends Message
    case class RemoveCallback(event: Event, callback: Callback) extends Message
  }
}
