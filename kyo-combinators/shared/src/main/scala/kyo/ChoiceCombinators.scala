package kyo

import kyo.debug.Debug
import kyo.kernel.ArrowEffect
import scala.annotation.tailrec
import scala.annotation.targetName
import scala.util.NotGiven

extension [A, S](effect: A < (S & Choice))

    /** Filters the result of this computation and performs a `Choice` effect if the condition does not hold.
      *
      * @return
      *   A computation that produces the result of this computation with Choice effect
      */
    def filterChoice[S1](predicate: A => Boolean < S1)(using Frame): A < (S & S1 & Choice) =
        effect.map(a => predicate(a).map(b => Choice.dropIf(!b)).andThen(a))

    /** Handles the Choice effect and returns its result as a `Seq[A]`.
      *
      * @return
      *   A computation that produces a sequence of results from this computation with the Choice effect handled
      */
    def handleChoice(using Frame): Seq[A] < S = Choice.run(effect)

    /** Handles dropped Choice effects as Abort[Absent]
      *
      * @return
      *   A computation that aborts with Maybe.Empty when its Choice effect is reduced to an empty sequence
      */
    def choiceDropToAbsent(using Frame): A < (Choice & Abort[Absent] & S) =
        Choice.run(effect).map:
            case seq if seq.isEmpty => Abort.fail(Absent)
            case other              => Choice.evalSeq(other)

    /** Handles dropped Choice effects as NoSuchElementException aborts
      *
      * @return
      *   A computation that aborts with Maybe.Empty when its Choice effect is reduced to an empty sequence
      */
    def choiceDropToThrowable(using Frame): A < (Choice & Abort[NoSuchElementException] & S) =
        Choice.run(effect).map:
            case seq if seq.isEmpty => Abort.catching(Chunk.empty.head)
            case other              => Choice.evalSeq(other)

    /** Handles dropped Choice effects as Aborts of a specified instance of E
      *
      * @param error
      *   Error that the resulting effect will abort with if Choice is reduced to an empty sequence
      * @return
      *   A computation that produces the result of this computation with dropped Choice translated to Abort[E]
      */
    def choiceDropToFailure[E](error: => E)(using Frame): A < (Choice & Abort[E] & S) =
        Choice.run(effect).map:
            case seq if seq.isEmpty => Abort.fail(error)
            case other              => Choice.evalSeq(other)

end extension
