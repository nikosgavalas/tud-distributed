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
}

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
        Thread.sleep(Random.between(1, Config.maxWorkTime))
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex : Int = -1

        var messageCounter: Int = 0

        var myVC : VectorClock = null
        var myEVC : EncVectorClock = null
        var myREVC : ResEncVectorClock = null

        context.log.info("ChildActor {} up", context.self.path.name)

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, childIndex) =>
                    allProcesses = peers
                    myIndex = childIndex

                    // each actor initializes their clocks
                    myVC = new VectorClock(myIndex, peers.length)
                    myEVC = new EncVectorClock(myIndex, peers.length)
                    myREVC = new ResEncVectorClock(myIndex, peers.length)

                    // context.log.info("{} received peers {}", context.self.path.name, peers)

                    if (childIndex == 0) {
                        // p0 sends an initial message to trigger the deliveries
                        myVC.localTick()
                        myEVC.localTick()
                        myREVC.localTick()
                        allProcesses(1) ! PeerMessage("init msg", myVC.getTimestamp(), myEVC.getTimestamp(), myREVC.getTimestamp())
                    }

                case PeerMessage(content, timestampVC, timestampEVC, timestampREVC) =>
                    context.log.info("{} received {} with timestamps {} {} {}", context.self.path.name, content, timestampVC, timestampEVC, timestampREVC)

                    // merge and tick
                    myVC.mergeWith(timestampVC)
                    myEVC.mergeWith(timestampEVC)
                    myREVC.mergeWith(timestampREVC)
                    myVC.localTick()
                    myEVC.localTick()
                    myREVC.localTick()

                    if (Config.doWork)
                        doWork()

                    val compVC = myVC.compareWith(timestampVC)
                    val compEVC = myEVC.compareWith(timestampEVC)
                    val compREVC = myREVC.compareWith(timestampREVC)

                    if (compVC != compEVC || compEVC != compREVC) {
                        println("clocks inconsistent")
                        sys.exit(1)
                    }

                    if (messageCounter < Config.maxMessagesPerChild) {
                        // tick and send
                        myVC.localTick()
                        myEVC.localTick()
                        myREVC.localTick()
                        allProcesses(Random.between(0, allProcesses.length)) ! PeerMessage("msg", myVC.getTimestamp(), myEVC.getTimestamp(), myREVC.getTimestamp())
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
