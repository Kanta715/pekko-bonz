package pekko

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

/** Actor のエラー処理
 *
 * Step1でも記述したがアクターは、アクターのライフサイクルに関連するシグナルを受け取ることができる
 * ここでは、アクターのエラー処理について確認する
 * 子アクターを監督する親アクターは、子アクター生成時に監督戦略を設定することができる（デフォルトでは停止）
 * - PostStop:    アクターが停止する直前に送信される   <- Default
 * - PreRestart:  アクターが再起動する直前に送信される
 */
object ActorStep2 extends App {
  val supervisingActor: ActorRef[String] = ActorSystem(SupervisingActor(), "SupervisingActor")
  supervisingActor ! "failChild"
}

/** 監督者アクター（親） */
object SupervisingActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisingActor(context))
}

class SupervisingActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  private val childRestart = context.spawn(
    // restart はデフォルトだと上限なく再起動するので注意（上限を設けるか、resumeなどで対応する）
    Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart),
    "supervised-actor"
  )
  println(s" childRestart: ************ $childRestart ************")

  private val childDefault = context.spawn(DefaultSupervisedActor(), "supervised-actor-default")
  println(s" childDefault: ************ $childDefault ************")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "failChild" =>
        println(
          s"""
            | ########### SupervisingActor before error ##########
            | ${this.context.children.toSeq}
            |""".stripMargin)
        childRestart ! "fail"
        childDefault ! "fail"
        Thread.sleep(2000)
        // 子アクターが停止して、削除されていることを親アクターは検知できない
        // 実際には、childDefault は削除されている
        println(
          s"""
            | ########### SupervisingActor after error ##########
            | ${this.context.children.toSeq}
            |""".stripMargin)
        childRestart ! "fail"
        childDefault ! "fail"
        this
    }
}

/** 監督されるアクター（子） */
object SupervisedActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisedActor(context))
}

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised actor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("!!!!!!!! supervised actor fails now")
        throw new Exception("I failed!")
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("~~~~~~~~ supervised actor will be restarted ~~~~~~~~~")
      this
    case PostStop =>
      println("++++++++++++ supervised actor stopped +++++++++++++")
      this
  }
}

object DefaultSupervisedActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new DefaultSupervisedActor(context))
}

class DefaultSupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("default supervised actor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("????????????? default supervised actor fails now")
        throw new Exception("I failed!")
    }
}
