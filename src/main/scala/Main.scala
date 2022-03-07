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
    // messages from the parent
    final case class ControlMessage(peers: Array[ActorRef[ChildActor.Message]], childIndex: Int) extends Message
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

    // helper function to print the clocks
    def timestampsToString(timestamps: List[Any]): String = {
        val ret: StringBuilder = new StringBuilder("")
        for (ts <- timestamps) {
            ts match {
                case ints: Array[Int] => ret ++= ints.mkString("[", ",", "]")
                case _ => ret ++= ts.toString
            }
            ret += '-'
        }
        ret.toString()
    }

    def apply(): Behavior[Message] = Behaviors.setup { context =>
        var myIndex : Int = -1
        var messageCounter: Int = 0

        val clocks: Clocks = new Clocks(Config.clocksActive)

        context.log.info(s"ChildActor ${context.self.path.name} up")

        Behaviors.receive { (context, message) =>
            message match {
                case ControlMessage(peers, childIndex) =>
                    allPeers = peers
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
                    context.log.info(s"${context.self.path.name} received ${content} with ${timestampsToString(timestamps)}")

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
                        clocks.tick()
                        allPeers(Random.between(0, allPeers.length)) ! PeerMessage("msg", clocks.getTimestamps())
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
