import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActor {
  sealed trait Message
  final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], childIndex: Int) extends Message  // messages from the parent
  final case class PeerMessage(content: String) extends Message   // messages to/from the other children

  var otherProcesses : Array[ActorRef[Message]] = Array()
  var myIndex : Int = -1
  var myClock : LogicalClock = null

  def apply(): Behavior[Message] = Behaviors.setup { context =>
    context.log.info("ChildActor {} up", context.self.path.name)

    Behaviors.receive { (context, message) =>
      message match {
        case ControlMessage(peers, childIndex) =>
          otherProcesses = peers
          myIndex = childIndex 
          myClock = new VectorClock(myIndex, peers.length)
          myClock.localTick() 
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
}

object ParentActor {
  final case class SpawnActors(number: Int)

  def apply(): Behavior[SpawnActors] = Behaviors.receive { (context, message) =>
    val processList = new Array[ActorRef[ChildActor.Message]](message.number)

    // upon receiving the message, spawn the children
    for (i <- 0 until message.number) {
      processList(i) = context.spawn(ChildActor(), "process" + i)
    }

    // send relevant information to each child
    for (j <- 0 until message.number) {
      processList(j) ! ControlMessage(processList, j)
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
