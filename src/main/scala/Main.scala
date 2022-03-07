import ChildActor.ControlMessage
import ParentActor.SpawnActors
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import scala.util.Random

object Config {
    val numberOfThreads: Int = 3
    val doWork: Boolean = true
    val maxWorkTime: Int = 20
    val maxMessagesPerChild: Int = 1000
    val clocksActive: List[String] = List[String]("VC", "EVC", "REVC")
}

object ChildActor {
    sealed trait Message
    final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], childIndex: Int) extends Message  // messages from the parent
    final case class PeerMessage(content: String, timestamps: List[Any]) extends Message   // messages to/from the other children

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

        val clocks: Clocks = new Clocks(Config.clocksActive)

        context.log.info("ChildActor {} up", context.self.path.name)

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, childIndex) =>
                    allPeers = peers
                    myIndex = childIndex

                    // each actor initializes their clocks
                    clocks.initialize(myIndex, peers.length)

                    // context.log.info("{} received peers {}", context.self.path.name, peers)

                    if (childIndex == 0) {
                        // p0 sends an initial message to trigger the deliveries
                        clocks.tick()
                        allPeers(1) ! PeerMessage("init msg", clocks.getTimestamps())
                    }

                case PeerMessage(content, timestamps) =>
                    context.log.info("{} received {} with timestamps {} {} {}", context.self.path.name, content, timestampVC, timestampEVC, timestampREVC)

                    // merge and tick
                    clocks.merge(timestamps)
                    clocks.tick()

                    if (Config.doWork)
                        doWork()

                    if (! clocks.allConsistent(timestamps)) {
                        println("clocks inconsistent")
                        sys.exit(1)
                    }

                    if (messageCounter < Config.maxMessagesPerChild) {
                        // tick and send
                        myVC.localTick()
                        myEVC.localTick()
                        myREVC.localTick()
                        allPeers(Random.between(0, allPeers.length)) ! PeerMessage("msg", myVC.getTimestamp(), myEVC.getTimestamp(), myREVC.getTimestamp())
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
            processList(i) = context.spawn(ChildActor(), "Process-" + i)
        }

        // send relevant information to each child
        for (j <- 0 until message.number) {
            processList(j) ! ControlMessage(processList, j)
        }

        Behaviors.same
    }
}

object Main extends App {
    // start the actor system
    val parentActor: ActorSystem[ParentActor.SpawnActors] = ActorSystem(ParentActor(), "Main")

    // send a message to ParentActor to spawn children
    parentActor ! SpawnActors(Config.numberOfThreads)
}
