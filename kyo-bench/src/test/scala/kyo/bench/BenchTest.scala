package kyo.bench

import kyo.*
import org.scalatest.Assertions
import org.scalatest.freespec.AsyncFreeSpec

class BenchTest extends AsyncFreeSpec with Assertions:

    def test[T](b: Bench.SyncAndFork[T], expected: T)(using CanEqual[T, T]): Unit =
        "cats" - {
            "sync" in {
                assert(b.syncCats() == expected)
            }
            "fork" in {
                assert(b.forkCats() == expected)
            }
        }
        "kyo" - {
            "sync" in {
                assert(b.syncKyo() == expected)
            }
            "fork" in {
                assert(b.forkKyo() == expected)
            }
        }
        "zio" - {
            "sync" in {
                assert(b.syncZio() == expected)
            }
            "fork" in {
                assert(b.forkZio() == expected)
            }
        }
    end test

    def test[T](b: Bench.ForkOnly[T], expected: T)(using CanEqual[T, T]): Unit =
        "cats" - {
            "fork" in {
                assert(b.forkCats() == expected)
            }
        }
        "kyo" - {
            "fork" in {
                assert(b.forkKyo() == expected)
            }
        }
        "zio" - {
            "fork" in {
                assert(b.forkZio() == expected)
            }
        }
    end test

    "BroadFlatMapBench" - {
        test(BroadFlatMapBench(), BigInt(610))
    }

    "ForkChainedBench" - {
        test(ForkChainedBench(), 0)
    }

    "CollectBench" - {
        val b = CollectBench()
        test(b, List.fill(b.count)(1))
    }

    "CollectParBench" - {
        val b = CollectParBench()
        test(b, List.fill(b.count)(1))
    }

    "CountdownLatchBench" - {
        test(CountdownLatchBench(), 0)
    }

    "DeepBindBench" - {
        test(DeepBindBench(), ())
    }

    "EnqueueDequeueBench" - {
        test(EnqueueDequeueBench(), ())
    }

    "ForkManyBench" - {
        test(ForkManyBench(), 0)
    }

    "NarrowBindBench" - {
        test(NarrowBindBench(), 10000)
    }

    "NarrowBindMapBench" - {
        test(NarrowBindMapBench(), 10000)
    }

    "PingPongBench" - {
        test(PingPongBench(), ())
    }

    "ProducerConsumerBench" - {
        test(ProducerConsumerBench(), ())
    }

    "SemaphoreBench" - {
        test(SemaphoreBench(), ())
    }

    "SemaphoreContentionBench" - {
        test(SemaphoreContentionBench(), ())
    }

    "SuspensionBench" - {
        test(SuspensionBench(), ())
    }

    "SchedulingBench" - {
        test(SchedulingBench(), 1001000)
    }

    "RendezvousBench" - {
        val b        = RendezvousBench()
        val expected = b.depth * (b.depth + 1) / 2
        test(b, expected)
    }

    "ForkSpawnBench" - {
        test(ForkSpawnBench(), ())
    }

    "MtlBench" in {
        assert(MtlBench().syncKyo().isRight)
    }

    "StreamBench" - {
        test(StreamBench(), 25000000)
    }

    "StreamBufferBench" - {
        test(StreamBufferBench(), 25000000)
    }

    "FailureBench" - {
        given [T]: CanEqual[T, T] = CanEqual.derived
        test(FailureBench(), Left(Ex2))
    }

    "RandomBench" - {
        test(RandomBench(), ())
    }

    "ForkJoinBench" - {
        test(ForkJoinBench(), ())
    }

    "ForkJoinContentionBench" - {
        test(ForkJoinContentionBench(), ())
    }
end BenchTest
