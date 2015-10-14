package better

import akka.actor._

import better.files._

package object files {
  import FileWatcher._
  implicit class FileWatcherOps(file: File) {
    def watcherProps(recursive: Boolean): Props = Props(new FileWatcher(file, recursive))

    def newWatcher(recursive: Boolean = true)(implicit system: ActorSystem): ActorRef = system.actorOf(watcherProps(recursive))
  }

  def when(events: Event*)(callback: Callback) = Message.RegisterCallback(events.distinct, callback)

  def on(event: Event)(callback: File => Unit) = when(event){case (`event`, file) => callback(file)}

  def stop(event: Event, callback: Callback) = Message.RemoveCallback(event, callback)

  import scala.collection.mutable
  private[files] def newMultiMap[A, B]: mutable.MultiMap[A, B] = new mutable.HashMap[A, mutable.Set[B]] with mutable.MultiMap[A, B]
}
