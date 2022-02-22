import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActor {
  final case class ControlMessage(content: String, peer: ActorRef[ControlMessage])
  // final case class PeerMessage(content: String, peer: ActorRef[PeerMessage])

  def apply(): Behavior[ControlMessage] = Behaviors.setup { context =>
    context.log.info("ChildActor {} up", context.self.path.name)

    Behaviors.receive { (context, message) =>
      context.log.info("ChildActor {} received {}", context.self.path.name, message.content)

      // message.peer ! PeerMessage("hi")

      Behaviors.same
    }
  }
}

object ParentActor {
  final case class SpawnActors(number: Int)

  def apply(): Behavior[SpawnActors] = Behaviors.receive { (context, message) =>
    // upon receiving the message, spawn the children
    val process1 = context.spawn(ChildActor(), "process1")
    val process2 = context.spawn(ChildActor(), "process2")

    process1 ! ControlMessage("hi", process2)
    // tell process1 to send a message to process2
    // process1 ! ChildActor.ControlMessage("log", process2) // can't get this to work

    Behaviors.same
  }
}

object Main extends App {
  val numberOfThreads: Int = 2

  // start the actor system
  val parentThread: ActorSystem[ParentActor.SpawnActors] = ActorSystem(ParentActor(), "Main")

  // send a message to ParentActor to spawn children
  parentThread ! SpawnActors(numberOfThreads)
}