package pekkoIot

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

object IotAppSupervisor {
  def apply: Behavior[Nothing] = Behaviors.setup[Nothing](new IotAppSupervisor(_))
}
class IotAppSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("IoT application started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    Behaviors.unhandled // 何もしない
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("IoT application stopped")
      this
  }
}

object IotApp {

  def main(args: Array[String]): Unit =
    ActorSystem[Nothing](IotAppSupervisor.apply, "iot-app")
}
