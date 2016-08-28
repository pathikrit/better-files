package better.files

object ActorSystemSupport {
  import akka.actor.ActorSystem
  implicit val system = ActorSystem()

  sys.addShutdownHook {
    system.terminate()
  }
}
