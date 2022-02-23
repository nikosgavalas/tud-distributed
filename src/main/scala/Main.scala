import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActor extends VectorClock {
  sealed trait Message
  final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]]) extends Message  // messages from the parent
  final case class PeerMessage(content: String) extends Message   // messages to/from the other children

  var otherProcesses : Array[ActorRef[Message]] = Array()

  def apply(): Behavior[Message] = Behaviors.setup { context =>
    context.log.info("ChildActor {} up", context.self.path.name)

    Behaviors.receive { (context, message) =>
      message match {
        case ControlMessage(peers) =>
          otherProcesses = peers
          context.log.info("{} received peers {}", context.self.path.name, peers)
          for (i <- otherProcesses) {
            if (i.path.name != context.self.path.name)  // send to all except self
              i ! PeerMessage(f"hi from ${context.self.path.name} to ${i.path.name}")
          }
        case PeerMessage(content) =>
          context.log.info("received {}", content)
      }
      Behaviors.same
    }
  }

  override def tick(str: String): Unit = ???

  override def merge(str: String): Unit = ???

  override def compare(str: String): Unit = ???
}

object ParentActor {
  final case class SpawnActors(number: Int)

  def apply(): Behavior[SpawnActors] = Behaviors.receive { (context, message) =>
    val processList = new Array[ActorRef[ChildActor.Message]](message.number)

    // upon receiving the message, spawn the children
    for (i <- 0 until message.number) {
      processList(i) = context.spawn(ChildActor(), "process" + i)
    }

    // send to each child an array of its peers
    for (j <- 0 until message.number) {
      processList(j) ! ControlMessage(processList)
    }

    Behaviors.same
  }
}

object Main extends App {
  val numberOfThreads: Int = 3

  // start the actor system
  val parentThread: ActorSystem[ParentActor.SpawnActors] = ActorSystem(ParentActor(), "Main")

  // send a message to ParentActor to spawn children
  parentThread ! SpawnActors(numberOfThreads)
}
