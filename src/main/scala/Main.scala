import ChildActor.ControlMessage
import Config.clocksActive
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random

object Config {
    val numberOfThreads: Int = 3
    val doWork: Boolean = false
    val maxWorkTime: Int = 20
    val maxMessagesPerChild: Int = 100
    val clocksActive: List[String] = List[String]("DMTREVC")
}

object ChildActor {
    sealed trait Message
    // messages from the parent
    final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], parentActor: ActorRef[ParentActor.Message], childIndex: Int) extends Message
    // messages to/from other children
    final case class PeerMessage(content: String, timestamps: List[Any]) extends Message

    var allPeers : Array[ActorRef[Message]] = Array()

    def broadcast(peerMessage: PeerMessage, context: ActorContext[Message]): Unit = {
        // send to all except self
        for (i <- allPeers) {
            if (i.path.name != context.self.path.name)
                i ! peerMessage
        }
    }

    def doWork(): Unit = {
        Thread.sleep(Random.between(1, Config.maxWorkTime))
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex : Int = -1
        var messageCounter: Int = 0
        var parentActor: ActorRef[ParentActor.Message] = null

        val clocks: Clocks = new Clocks(Config.clocksActive)

        context.log.info(s"ChildActor ${context.self.path.name} up")

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, parent, childIndex) =>
                    allPeers = peers
                    parentActor = parent
                    myIndex = childIndex

                    // each actor initializes their clocks
                    clocks.initialize(myIndex, peers.length)

                    // context.log.info(s"${context.self.path.name} received peers ${peers}")

                    if (childIndex == 0) {
                        // p0 sends an initial message to trigger the deliveries
                        clocks.tick()
                        allPeers(1) ! PeerMessage("init msg", clocks.getTimestamps())
                    }

                case PeerMessage(content, timestamps) =>
                    context.log.info(s"${context.self.path.name} received '${content}' with timestamps: ${clocksActive.zip(timestamps)}")

                    // merge and tick
                    clocks.merge(timestamps)
                    clocks.tick()

                    if (Config.doWork)
                        doWork()

                    if (! clocks.allConsistent(timestamps)) {
                        println("clocks inconsistent")
                        sys.exit(1)
                    }

                    // tick and send
                    clocks.tick()
                    val receivingPeer = Random.between(0, allPeers.length);
                    // temporary hack
                    if (clocksActive.contains("DMTREVC")) {
                        clocks.clocks.foreach {
                            case clock: DMTResEncVectorClock =>
                                clock.mergedInto(receivingPeer)
                            case _ =>
                        }
                    }
                    allPeers(receivingPeer) ! PeerMessage("msg", clocks.getTimestamps())

                    messageCounter += 1

                    if (messageCounter == Config.maxMessagesPerChild) {
                        parentActor ! ParentActor.ChildDone()
                    }
            }

            Behaviors.same
        }
    }
}

object ParentActor {
    sealed trait Message
    final case class SpawnActors(number: Int) extends Message
    final case class ChildDone() extends Message

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var numberOfChildren = 0
        var childrenDone = 0
        var processList: Array[ActorRef[ChildActor.Message]] = null
        val startTime = System.nanoTime

        Behaviors.receive { (context, message) =>
            message match {
                case SpawnActors(number) =>
                    numberOfChildren = number
                    processList = new Array[ActorRef[ChildActor.Message]](numberOfChildren)

                    // upon receiving the message, spawn the children
                    for (i <- 0 until numberOfChildren) {
                        processList(i) = context.spawn(ChildActor(), "Process-" + i)
                    }

                    // send relevant information to each child
                    for (j <- 0 until numberOfChildren) {
                        processList(j) ! ControlMessage(processList, context.self, j)
                    }

                case ChildDone() =>
                    // when all children finish, exit.
                    childrenDone += 1
                    if (childrenDone == numberOfChildren) {
                        for (process <- processList) {
                            context.stop(process)
                        }
                        println(s"duration: ${(System.nanoTime - startTime) / 1e9d} seconds")
                        sys.exit(0)
                    }
            }
            Behaviors.same
        }
    }
}

object Main extends App {
    // start the actor system
    val parentActor: ActorSystem[ParentActor.SpawnActors] = ActorSystem(ParentActor(), "Main")

    // send a message to ParentActor to spawn children
    parentActor ! SpawnActors(Config.numberOfThreads)
}
