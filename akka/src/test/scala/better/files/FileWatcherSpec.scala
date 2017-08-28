package better.files

import Dsl._

import scala.concurrent.duration._
import scala.language.postfixOps

class FileWatcherSpec extends CommonSpec {
  "file watcher" should "watch directories" in {
    assume(isCI)
    File.usingTemporaryDirectory() {dir =>
      (dir / "a" / "b" / "c.txt").createIfNotExists(createParents = true)

      var log = List.empty[String]
      def output(file: File, event: String) = synchronized {
        val msg = s"${dir.path relativize file.path} got $event"
        println(msg)
        log = msg :: log
      }
      /***************************************************************************/
      import java.nio.file.{StandardWatchEventKinds => Events}
      import FileWatcher._

      import akka.actor.{ActorRef, ActorSystem}
      implicit val system = ActorSystem()

      val watcher: ActorRef = dir.newWatcher()

      watcher ! when(events = Events.ENTRY_CREATE, Events.ENTRY_MODIFY) {   // watch for multiple events
        case (Events.ENTRY_CREATE, file) => output(file, "created")
        case (Events.ENTRY_MODIFY, file) => output(file, "modified")
      }

      watcher ! on(Events.ENTRY_DELETE)(file => output(file, "deleted"))    // register partial function for single event
      /***************************************************************************/
      sleep(5 seconds)
      (dir / "a" / "b" / "c.txt").writeText("Hello world"); sleep()
      rm(dir / "a" / "b"); sleep()
      mkdir(dir / "d"); sleep()
      touch(dir / "d" / "e.txt"); sleep()
      mkdirs(dir / "d" / "f" / "g"); sleep()
      touch(dir / "d" / "f" / "g" / "e.txt"); sleep()
      (dir / "d" / "f" / "g" / "e.txt") moveTo (dir / "a" / "e.txt"); sleep()
      sleep(10 seconds)

      val expectedEvents = List(
        "a/e.txt got created", "d/f/g/e.txt got deleted", "d/f/g/e.txt got modified", "d/f/g/e.txt got created", "d/f got created",
        "d/e.txt got modified", "d/e.txt got created", "d got created", "a/b got deleted", "a/b/c.txt got deleted", "a/b/c.txt got modified"
      )

      expectedEvents diff log shouldBe empty

      system.terminate()
    }
  }
}
