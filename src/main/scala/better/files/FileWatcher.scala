package better.files

import java.nio.file.{StandardWatchEventKinds, WatchKey, Path, WatchEvent}

import akka.actor.Actor

import scala.collection.JavaConversions._
import scala.language.existentials
import scala.util.control.Exception

/**
 * An actor that can watch a file or a directory
 * Instead of directly calling the constructor of this, call file.newWatcher to create the actor
 *
 * @param file watch this file (or directory)
 * @param maxDepth In case of directories, how much depth should we watch
 */
class FileWatcher(file: File, maxDepth: Int = 0) extends Actor {

  def this(file: File, recursive: Boolean) = this(file, if (recursive) Int.MaxValue else 0)

  protected[FileWatcher] val service = file.newWatchService

  protected[FileWatcher] val callbacks = newMultiMap[FileWatcher.Event, FileWatcher.Callback]

  protected[FileWatcher] val watcher = new Thread {
    override def run() = Exception.ignoring(classOf[InterruptedException]) {
      Iterator.continually(service.take()) foreach process
    }

    def process(key: WatchKey) = {
      val root = key.watchable().asInstanceOf[Path]
      key.pollEvents() foreach {event =>
        val relativePath = event.context().asInstanceOf[Path]
        self ! (event.kind() -> File(root resolve relativePath))
      }
      key.reset()
    }
  }

  override def preStart() = {
    super.preStart()
    def watch(aFile: File) = {
      val events = Seq(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
      if (aFile.isDirectory) {
        for {
          f <- aFile.walk(maxDepth) if f.isDirectory && f.exists
        } f.path.register(service, events: _*)
      } else if (aFile.exists) {   // There is no way to watch a regular file; so watch its parent instead
        aFile.parent.path.register(service, events: _*)
      }
    }
    callbacks.addBinding(StandardWatchEventKinds.ENTRY_CREATE, {      //TODO: self ! on(StandardWatchEventKinds.ENTRY_CREATE)(watch)
      case (_, newFile) => watch(newFile)
    })
    watch(file)
    watcher.setDaemon(true)
    watcher.start()
  }

  override def receive = {
    case (event: FileWatcher.Event, target: File) if (callbacks contains event) && (file.isDirectory || (file isSamePathAs target)) =>
      callbacks(event) foreach {f => f(event -> target)}
    case FileWatcher.RegisterCallback(events, callback) =>
      events foreach {event => callbacks.addBinding(event, callback)}
  }

  override def postStop() = {
    super.postStop()
    watcher.interrupt()
    service.close()
  }
}

object FileWatcher {
  type Event = WatchEvent.Kind[_]
  type FileEvent = (Event, File)
  type Callback = PartialFunction[FileEvent, Unit]

  case class RegisterCallback(events: Seq[Event], callback: Callback)
  //TODO: DeRegisterCallback/UnwatchEvent - typedActor?
}
