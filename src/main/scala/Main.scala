import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random

object ChildActor {
    sealed trait Message
    final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], childIndex: Int) extends Message  // messages from the parent
    final case class PeerMessage(content: String, timestampVC: Any, timestampEVC: Any, timestampREVC: Any) extends Message   // messages to/from the other children

    var allProcesses : Array[ActorRef[Message]] = Array()

    def broadcast(peerMessage: PeerMessage, context: ActorContext[Message]): Unit = {
        for (i <- allProcesses) {
            if (i.path.name != context.self.path.name)  // send to all except self
                i ! peerMessage
        }
    }

    def doWork(): Unit = {
        Thread.sleep(Random.between(1, 100))
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex : Int = -1

        var messageCounter: Int = 0
        val maxNumberOfMessages: Int = 1000

        var myVC : VectorClock = null
        var myEVC : EncVectorClock = null
        var myREVC : DMTResEncVectorClock = null

        context.log.info("ChildActor {} up", context.self.path.name)

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, childIndex) =>
                    allProcesses = peers
                    myIndex = childIndex

                    myVC = new VectorClock(myIndex, peers.length)
                    myEVC = new EncVectorClock(myIndex, peers.length)
                    myREVC = new DMTResEncVectorClock(myIndex, peers.length)

                    // context.log.info("{} received peers {}", context.self.path.name, peers)

                    if (childIndex == 0) {
                        myVC.localTick()
                        myEVC.localTick()
                        myREVC.localTick()

                        allProcesses(1) ! PeerMessage("init msg", myVC.getTimestamp(), myEVC.getTimestamp(), myREVC.getTimestamp())  // p1 sends an initial message to trigger the cyclic delivery
                    }

                case PeerMessage(content, timestampVC, timestampEVC, timestampREVC) =>
                    // context.log.info("{} received {} with timestamps {} {} {}", context.self.path.name, content, timestampVC, timestampEVC, timestampREVC)
                    
                    myVC.mergeWith(timestampVC)
                    myEVC.mergeWith(timestampEVC)
                    myREVC.mergeWith(timestampREVC)
                    myVC.localTick()
                    myEVC.localTick()
                    myREVC.localTick()

                    doWork()

                    val compVC = myVC.compareWith(timestampVC)
                    val compEVC = myEVC.compareWith(timestampEVC)
                    val compREVC = myREVC.compareWith(timestampREVC)

                    if (compVC != compEVC || compEVC != compREVC) {
                        println("clocks inconsistent")
                        sys.exit(1)
                    }

                    if (messageCounter < maxNumberOfMessages) {
                        myVC.localTick()
                        myEVC.localTick()
                        myREVC.localTick()
                        val receivingPeer = Random.between(0, allProcesses.length)
                        myREVC.mergedInto(receivingPeer)
                        allProcesses(receivingPeer) ! PeerMessage("msg", myVC.getTimestamp(), myEVC.getTimestamp(), myREVC.getTimestamp())
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
