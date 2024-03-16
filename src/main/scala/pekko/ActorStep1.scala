package pekko

import org.apache.pekko.actor
import actor.typed._
import actor.typed.scaladsl._

/** Actor の起動と停止
 *
 *  Actor にはライフサイクルシグナルが存在する
 *  親と子の Actor は、常にライフサイクルを通して接続されている（親は常に子を監視している）
 *  アクター定義で onSignal メソッドをオーバーライドすることで、アクターのライフサイクルに関連するシグナルを受け取ることができる
 *  - PostStop:    アクターが停止する直前に送信される <- Step1 で確認（それ以外は Step2 以降で確認）
 *  - PreRestart:  アクターが再起動する直前に送信される
 *  - PostRestart: アクターが再起動した直後に送信される
 *  - Terminated:  監視対象のアクターが停止したときに送信される
 *  - Signal:      上記以外のシグナル
 *
 *  Step1では、アクターの起動と、アクター間でのメッセージのやり取り、アクターの停止を確認する
 *  1. Behaviors.setup でUserGuardianActor を起動
 *  2. UserGuardianActor にメッセージを送信
 *  3. UserGuardianActor が FirstActor を起動
 *  4. FirstActor にメッセージを送信
 *  5. FirstActor が SecondActor を起動
 *  6. SecondActor にメッセージを送信
 *  7. SecondActor が SomeActor を起動
 *  8. SomeActor にメッセージを送信 (ただし、Behaviors.empty は空のアクターなので、メッセージは何も受け取らない)
 *  9. ユーザーガーディアンアクターにメッセージを送信（Stop）
 *  10. ユーザーガーディアンアクターが停止（子アクターの停止を待ってから停止）
 *  11. FirstActor が PostStop シグナルを受け取り停止（SecondActor の停止を待ってから停止）
 *  12. SecondActor が PostStop シグナルを受け取り停止
 */
object ActorStep1 extends App {
  // 1. UserGuardianActor を起動
  val userGuardian = ActorSystem(UserGuardianActor(), "UserGuardianActor")
  println(s" userGuardian: ************ $userGuardian ************")
  // 2. UserGuardianActor にメッセージを送信
  userGuardian ! "Hello"
  // 9. UserGuardianActor にメッセージを送信
  userGuardian ! "Stop"
}

object UserGuardianActor {
  def apply(): Behavior[String] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage { msg =>
        msg match {
          case "Stop" =>
            println(
              s"""
                | ########### UserGuardianActor before stopped ##########
                | ${context.children.toSeq}
                |""".stripMargin)
            // 10. UserGuardianActor が停止（子アクターの停止を待ってから停止）
            Behaviors.stopped(() => {
              println(
                s"""
                  | ########### UserGuardianActor after stopped ##########
                  | ${context.children.toSeq}
                  |""".stripMargin) // 停止したアクターは消滅する
            })
          case _ =>
            println(s" Received message: ************ $msg ************")
            // 3. FirstActor を起動
            val firstActor: ActorRef[String] = context.spawn(FirstActor.apply(), "FirstActor")
            // 4. FirstActor にメッセージを送信
            firstActor ! (msg)
            println(s" FirstActor: +++++++++++++ $firstActor ++++++++++++++")
            Behaviors.same
        }
      }
    }
}

object FirstActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new FirstActor(context))
}

private class FirstActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] = {
    println(s" Received message: ========== $msg ==========")
    // 5. SecondActor を起動
    val secondActor: ActorRef[String] = context.spawn(SecondActor.apply(), "SecondActor")
    // 6. SecondActor にメッセージを送信
    secondActor ! (msg)
    println(s" SecondActor: +++++++++++++ $secondActor ++++++++++++++")
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println(" FirstActor stopped") // 11. FirstActor が停止
      this
  }
}

object SecondActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SecondActor(context))
}

private class SecondActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] = {
    println(s" Received message: ~~~~~~~~~~~~ $msg ~~~~~~~~~~~~~")
    // 7. SomeActor を起動
    val someActor: ActorRef[String] = context.spawn(Behaviors.empty[String], "SomeActor")
    // 8. SomeActor にメッセージを送信。ただし、Behaviors.empty は空のアクターなので、メッセージは何も受け取らない
    someActor ! (msg)
    println(s" SomeActor: +++++++++++++ $someActor ++++++++++++++")
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println(" SecondActor stopped") // 12. SecondActor が停止
      this
  }
}

