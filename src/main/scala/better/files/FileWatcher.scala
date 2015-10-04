package better.files

import java.nio.file.{WatchKey, Path, WatchEvent}

import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.language.existentials

/**
 * An actor that can watch a file or a directory
 *
 * @param file watch this file (or directory)
 * @param maxDepth In case of directories, how much depth should we watch
 */
class FileWatcher(file: File, maxDepth: Int = 0) extends Actor with ActorLogging {

  def this(file: File, recursive: Boolean) = this(file, if (recursive) Int.MaxValue else 0)

  protected[FileWatcher] val service = file.newWatchService

  protected[FileWatcher] val callbacks = mutable.Map.empty[FileWatcher.FileEvent, Set[FileWatcher.Callback]] withDefaultValue Set.empty

  private[FileWatcher] val watcher = new Runnable {
    override def run() = try {
      while(!Thread.currentThread().isInterrupted){
        process(service.take())
      }
    } catch {
      case e: InterruptedException =>
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

  protected[FileWatcher] val thread = new Thread(watcher)

  override def preStart() = {
    super.preStart()
    thread.setDaemon(true)
    thread.start()
  }

  override def receive = {
    case (event: FileWatcher.Event, file: File) => callbacks(event -> file) foreach {f => f(event -> file)}

    case FileWatcher.RegisterCallback(events, callback) if file.isDirectory =>  for {
      subDirectory <- file.walk(maxDepth) if subDirectory.isDirectory && subDirectory.exists
      _ = subDirectory.path.register(service, events : _*)
      event <- events
      fileEvent = event -> subDirectory
    } callbacks(fileEvent) = callbacks(fileEvent) + callback

    //TODO: handle watching files
  }

  override def postStop() = {
    super.postStop()
    thread.interrupt()
    service.close()
  }
}

object FileWatcher {
  type Event = WatchEvent.Kind[_]
  type FileEvent = (Event, File)
  type Callback = PartialFunction[FileEvent, Unit]

  case class RegisterCallback(events: Seq[Event], callback: Callback)
}
