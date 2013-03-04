package org.w3

import akka.actor._
import play.api.libs.iteratee._
import scala.concurrent._
import scala.concurrent.duration._

/** The following invariant does not hold:
  *   Given (enumerator, channel) = Concurrent.broadcast[Int]
  *   any element pushed in the channel is seen in the enumerator
  */
object Main {

  var acc = List.empty[Int]

  // the enumerator will just propagate all Int-s sent to the actor
  val (enumerator, channel) = Concurrent.broadcast[Int]
  class MyActor extends Actor {
    def receive = {
      case i: Int =>
        acc ::= i        // accumulates what we've seen so far
        channel.push(i)
      case () => channel.eofAndEnd()
    }
  }

  val system = ActorSystem("MySystem")

  val myActor = system.actorOf(Props[MyActor], name = "myactor")

  def main (args: Array[String]): Unit = {
   
    // we're sending all the Ints from 1 to 10000
    val list = (1 to 10000).toList 
    list.foreach { i => myActor ! i }
    myActor ! ()

    val f = enumerator |>>> Iteratee.getChunks[Int]
    val result = Await.result(f, Duration(1, "s"))

    // prints 10000, so we've seen all the messages
    println(acc.size)

    // problem here: does not print 10000, some messages are lost
    println(result.size)

    system.shutdown()

  }

}
