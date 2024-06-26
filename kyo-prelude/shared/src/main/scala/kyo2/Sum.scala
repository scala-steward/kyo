package kyo2

import kyo.Tag
import kyo2.*
import kyo2.kernel.*

sealed trait Sum[V] extends Effect[Const[V], Const[Unit]]

object Sum:

    inline def add[V](inline v: V)(using inline tag: Tag[Sum[V]]): Unit < Sum[V] =
        Effect.suspend[Any](tag, v)

    final class RunOps[V](dummy: Unit) extends AnyVal:
        def apply[T, S](v: T < (Sum[V] & S))(
            using
            tag: Tag[Sum[V]],
            frame: Frame
        ): (Chunk[V], T) < S =
            Effect.handle.state(tag, Chunk.empty[V], v)(
                handle = [C] => (input, state, cont) => (state.append(input), cont(())),
                done = (state, res) => (state, res)
            )
    end RunOps

    inline def run[V >: Nothing]: RunOps[V] = RunOps(())

end Sum
