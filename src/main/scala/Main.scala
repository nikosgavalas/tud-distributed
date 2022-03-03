import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random

object ChildActor {
    sealed trait Message
    final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], childIndex: Int) extends Message  // messages from the parent
    final case class PeerMessage(content: String, timestamp: Any) extends Message   // messages to/from the other children

    var allProcesses : Array[ActorRef[Message]] = Array()

    def broadcast(peerMessage: PeerMessage, context: ActorContext[Message]): Unit = {
        for (i <- allProcesses) {
            if (i.path.name != context.self.path.name)  // send to all except self
                i ! peerMessage
        }
    }

    def doWork(): Unit = {
        Thread.sleep(Random.between(1, 200))
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex : Int = -1
        var myClock : LogicalClock = null
        var messageCounter: Int = 0

        context.log.info("ChildActor {} up", context.self.path.name)

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, childIndex) =>
                    allProcesses = peers
                    myIndex = childIndex
                    myClock = new ResEncVectorClock(myIndex, peers.length)

                    context.log.info("{} received peers {}", context.self.path.name, peers)

                    if (childIndex == 0) {
                        myClock.localTick()
                        allProcesses(1) ! PeerMessage("init msg", myClock.getTimestamp())  // p1 sends an initial message to trigger the cyclic delivery
                    }

                case PeerMessage(content, timestamp) =>
                    context.log.info("{} received {} with timestamp {}", context.self.path.name, content, timestamp)
                    
                    myClock.mergeWith(timestamp)
                    myClock.localTick()

                    if (messageCounter < 2) {
                        myClock.localTick()
                        allProcesses((myIndex + 1) % allProcesses.length) ! PeerMessage("msg", myClock.getTimestamp())
                    }

                    messageCounter += 1
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
