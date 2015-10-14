package better.files

import akka.actor._

/**
 * An actor that can watch a file or a directory
 * Instead of directly calling the constructor of this, call file.newWatcher to create the actor
 *
 * @param file watch this file (or directory)
 * @param maxDepth In case of directories, how much depth should we watch
 */
class FileWatcher(file: File, maxDepth: Int) extends FileMonitor(file, maxDepth) with Actor {
  import FileWatcher._
  protected[this] val callbacks = newMultiMap[Event, Callback]

  def this(file: File, recursive: Boolean = true) = this(file, if (recursive) Int.MaxValue else 0)

  override def dispatch(event: Event, file: File) = self ! Message.NewEvent(event, file)
  override def onException(exception: Throwable) = self ! Status.Failure(exception)

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
  import scala.collection.mutable

  type Event = WatchEvent.Kind[Path]
  type Callback = PartialFunction[(Event, File), Unit]

  sealed trait Message
  object Message {
    case class NewEvent(event: Event, file: File) extends Message
    case class RegisterCallback(events: Seq[Event], callback: Callback) extends Message
    case class RemoveCallback(event: Event, callback: Callback) extends Message
  }

  implicit class FileWatcherOps(file: File) {
    def watcherProps(recursive: Boolean): Props = Props(new FileWatcher(file, recursive))
    def newWatcher(recursive: Boolean = true)(implicit system: ActorSystem): ActorRef = system.actorOf(watcherProps(recursive))
  }

  def when(events: FileWatcher.Event*)(callback: FileWatcher.Callback) = FileWatcher.Message.RegisterCallback(events.distinct, callback)
  def on(event: FileWatcher.Event)(callback: (File => Unit)) = when(event){case (`event`, file) => callback(file)}
  def stop(event: FileWatcher.Event, callback: FileWatcher.Callback) = FileWatcher.Message.RemoveCallback(event, callback)

  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] = new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]
}
