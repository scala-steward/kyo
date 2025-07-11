package kyo

class AbortTest extends Test:

    case class Ex1() extends RuntimeException derives CanEqual
    case class Ex2() derives CanEqual

    val ex1 = new Ex1
    val ex2 = new Ex2

    "pure" - {
        "handle" in {
            assert(
                Abort.run[Ex1](Abort.get[Ex1](Right(1))).eval ==
                    Result.succeed(1)
            )
        }
        "handle + transform" in {
            assert(
                Abort.run[Ex1](Abort.get[Ex1](Right(1)).map(_ + 1)).eval ==
                    Result.succeed(2)
            )
        }
        "handle + effectful transform" in {
            assert(
                Abort.run[Ex1](Abort.get[Ex1](Right(1)).map(i =>
                    Abort.get[Ex1](Right(i + 1))
                )).eval ==
                    Result.succeed(2)
            )
        }
        "handle + transform + effectful transform" in {
            assert(
                Abort.run[Ex1](Abort.get[Ex1](Right(1)).map(_ + 1).map(i =>
                    Abort.get[Ex1](Right(i + 1))
                )).eval ==
                    Result.succeed(3)
            )
        }
        "handle + transform + failed effectful transform" in {
            val fail = Left[Ex1, Int](ex1)
            assert(
                Abort.run[Ex1](Abort.get[Ex1](Right(1)).map(_ + 1).map(_ =>
                    Abort.get(fail)
                )).eval ==
                    Result.fail(ex1)
            )
        }
        "union tags" - {
            "in suspend 1" in {
                val effect1: Int < Abort[String | Boolean] =
                    Abort.fail("failure")
                val handled1: Result[String, Int] < Abort[Boolean] =
                    Abort.run[String](effect1)
                val handled2: Result[Boolean, Result[String, Int]] =
                    Abort.run[Boolean](handled1).eval
                assert(handled2 == Result.succeed(Result.fail("failure")))
            }
            "in suspend 2" in {
                val effect1: Int < Abort[String | Boolean] =
                    Abort.fail("failure")
                val handled1: Result[Boolean, Int] < Abort[String] =
                    Abort.run[Boolean](effect1)
                val handled2: Result[String, Result[Boolean, Int]] =
                    Abort.run[String](handled1).eval
                assert(handled2 == Result.fail("failure"))
            }
            "in handle" in {
                val effect: Int < Abort[String | Boolean] =
                    Abort.fail("failure")
                val handled = Abort.run[String | Boolean](effect).eval
                assert(handled == Result.fail("failure"))
            }
        }
        "try" in {
            import scala.util.Try

            assert(Abort.run(Abort.get(Try(throw ex1))).eval == Result.fail(ex1))
            assert(Abort.run(Abort.get(Try("success!"))).eval == Result.succeed("success!"))
        }
    }

    "get" - {
        "either" in {
            assert(Abort.run(Abort.get(Left(1))).eval == Result.fail(1))
            assert(Abort.run(Abort.get[Ex1](Right(1))).eval == Result.succeed(1))
        }
        "result" in {
            assert(Abort.run(Abort.get(Result.succeed[Ex1, Int](1))).eval == Result.succeed(1))
            assert(Abort.run(Abort.get(Result.fail(ex1))).eval == Result.fail(ex1))
        }
        "option" in {
            assert(Abort.run(Abort.get(Option.empty)).eval == Result.fail(Absent))
            assert(Abort.run(Abort.get(Some(1))).eval == Result.succeed(1))
        }
        "maybe" in {
            assert(Abort.run(Abort.get(Maybe.empty)).eval == Result.fail(Absent))
            assert(Abort.run(Abort.get(Maybe(1))).eval == Result.succeed(1))
        }
    }

    "effectful" - {
        "success" - {
            val v: Int < Abort[Ex1] = Abort.get[Ex1](Right(1))
            "handle" in {
                assert(
                    Abort.run[Ex1](v).eval ==
                        Result.succeed(1)
                )
            }
            "handle + transform" in {
                assert(
                    Abort.run[Ex1](v.map(_ + 1)).eval ==
                        Result.succeed(2)
                )
            }
            "handle + effectful transform" in {
                assert(
                    Abort.run[Ex1](v.map(i => Abort.get[Ex1](Right(i + 1)))).eval ==
                        Result.succeed(2)
                )
            }
            "handle + transform + effectful transform" in {
                assert(
                    Abort.run[Ex1](v.map(_ + 1).map(i => Abort.get[Ex1](Right(i + 1)))).eval ==
                        Result.succeed(3)
                )
            }
            "handle + transform + failed effectful transform" in {
                val fail = Left[Ex1, Int](ex1)
                assert(
                    Abort.run[Ex1](v.map(_ + 1).map(_ => Abort.get(fail))).eval ==
                        Result.fail(ex1)
                )
            }
        }
        "failure" - {
            val v: Int < Abort[Ex1] = Abort.get(Left(ex1))
            "handle" in {
                assert(
                    Abort.run[Ex1](v).eval ==
                        Result.fail(ex1)
                )
            }
            "handle + transform" in {
                assert(
                    Abort.run[Ex1](v.map(_ + 1)).eval ==
                        Result.fail(ex1)
                )
            }
            "handle + effectful transform" in {
                assert(
                    Abort.run[Ex1](v.map(i => Abort.get[Ex1](Right(i + 1)))).eval ==
                        Result.fail(ex1)
                )
            }
            "handle + transform + effectful transform" in {
                assert(
                    Abort.run[Ex1](v.map(_ + 1).map(i => Abort.get[Ex1](Right(i + 1)))).eval ==
                        Result.fail(ex1)
                )
            }
            "handle + transform + failed effectful transform" in {
                val fail = Left[Ex1, Int](ex1)
                assert(
                    Abort.run[Ex1](v.map(_ + 1).map(_ => Abort.get(fail))).eval ==
                        Result.fail(ex1)
                )
            }
        }
    }

    "Abort" - {
        def test(v: Int): Int < Abort[Ex1] =
            v match
                case 0 =>
                    Abort.fail(ex1)
                case i => 10 / i
        "run" - {
            "success" in {
                assert(
                    Abort.run[Ex1](test(2)).eval ==
                        Result.succeed(5)
                )
            }
            "failure" in {
                assert(
                    Abort.run[Ex1](test(0)).eval ==
                        Result.fail(ex1)
                )
            }
            "panic" in {
                val p = new Exception
                assert(
                    Abort.run[Ex1](throw p).eval ==
                        Result.panic(p)
                )
            }
            "suspension + panic" in {
                val p = new Exception
                assert(
                    Abort.run[Ex1](Abort.get(Right(1)).map(_ => throw p)).eval ==
                        Result.panic(p)
                )
            }
            "inference" in {
                def t1(v: Int < Abort[Int | String]) =
                    Abort.run[Int](v)
                val _: Result[Int, Int] < Abort[String] =
                    t1(42)
                def t2(v: Int < (Abort[Int] & Abort[String])) =
                    Abort.run[String](v)
                val _: Result[String, Int] < Abort[Int] =
                    t2(42)
                def t3(v: Int < Abort[Int]) =
                    Abort.run[Int](v).eval
                val _: Result[Int, Int] =
                    t3(42)
                succeed
            }
            "super" in {
                val ex                        = new Exception
                val a: Int < Abort[Exception] = Abort.fail(ex)
                val b: Result[Throwable, Int] = Abort.run[Throwable](a).eval
                assert(b == Result.fail(ex))
            }
            "super success" in {
                val a: Int < Abort[Exception] = 24
                val b: Result[Throwable, Int] = Abort.run[Throwable](a).eval
                assert(b == Result.succeed(24))
            }
            "reduce large union incrementally" in {
                val t1: Int < Abort[Int | String | Boolean | Float | Char | Double] =
                    18
                val t2 = Abort.run[Int](t1)
                val t3 = Abort.run[String](t2)
                val t4 = Abort.run[Boolean](t3)
                val t5 = Abort.run[Float](t4)
                val t6 = Abort.run[Char](t5)
                val t7 = Abort.run[Double](t6)
                assert(t7.eval == Result.succeed(Result.succeed(Result.succeed(Result.succeed(Result.succeed(Result.succeed(18)))))))
            }
            "reduce large union in a single expression" in {
                val t: Int < Abort[Int | String | Boolean | Float | Char | Double] = 18
                // NB: Adding type annotation leads to compilation error
                val res =
                    Abort.run[Double](
                        Abort.run[Char](
                            Abort.run[Float](
                                Abort.run[Boolean](
                                    Abort.run[String](
                                        Abort.run[Int](t)
                                    )
                                )
                            )
                        )
                    ).eval
                val expected: Result[Double, Result[Char, Result[Float, Result[Boolean, Result[String, Result[Int, Int]]]]]] =
                    Result.succeed(Result.succeed(Result.succeed(Result.succeed(Result.succeed(Result.succeed(18))))))
                assert(res == expected)
            }
            "doesn't produce Fail if E isn't Throwable" in run {
                val ex = new Exception
                Abort.run[Any](throw ex).map(result => assert(result == Result.panic(ex)))
            }
            "nested panic to fail" - {
                "catches matching throwable as Failure" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run[RuntimeException](throw ex).eval
                    assert(result == Result.Failure(ex))
                }

                "does not convert matching Panic to Failure" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run[RuntimeException](Abort.panic(ex)).eval
                    assert(result == Result.Panic(ex))
                }

                "leaves non-matching Panic as Panic" in {
                    val ex     = new IllegalArgumentException("Test exception")
                    val result = Abort.run[NoSuchElementException](Abort.panic(ex)).eval
                    assert(result == Result.panic(ex))
                }

                "doesn't affect Success" in {
                    val result = Abort.run[RuntimeException](42).eval
                    assert(result == Result.succeed(42))
                }

                "doesn't affect Fail" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run[RuntimeException](Abort.fail(ex)).eval
                    assert(result == Result.Failure(ex))
                }

                "works with nested Aborts" in {
                    val ex = new RuntimeException("Inner exception")
                    val nested = Abort.run[IllegalArgumentException] {
                        Abort.run[RuntimeException](Abort.panic(ex))
                    }
                    val result = nested.eval
                    assert(result == Result.Success(Result.Panic(ex)))
                }
            }
        }
        "runPartial" - {
            "success" in {
                assert(
                    Abort.run(Abort.runPartial[Ex1](test(2))).eval ==
                        Result.Success(Result.Success(5))
                )
            }
            "failure" in {
                assert(
                    Abort.run(Abort.runPartial[Ex1](test(0))).eval ==
                        Result.Success(Result.Failure(ex1))
                )
            }
            "panic" in {
                val p = new Exception
                assert(
                    Abort.run(Abort.runPartial[Ex1](throw p)).eval == Result.Panic(p)
                )
            }
            "suspension + panic" in {
                val p = new Exception
                assert(
                    Abort.run(Abort.runPartial[Ex1](Abort.get(Right(1)).map(_ => Abort.panic(p)))).eval ==
                        Result.panic(p)
                )
            }
            "inference" in {
                def t1(v: Int < Abort[Int | String]) =
                    Abort.runPartial[Int](v)
                val _: Result.Partial[Int, Int] < Abort[String] =
                    t1(42)
                def t2(v: Int < (Abort[Int] & Abort[String])) =
                    Abort.runPartial[String](v)
                val _: Result.Partial[String, Int] < Abort[Int] =
                    t2(42)
                def t3(v: Int < Abort[Int]) =
                    Abort.runPartial[Int](v)
                val _: Result.Partial[Int, Int] < Abort[Nothing] =
                    t3(42)
                succeed
            }
            "super" in {
                val ex                                                 = new Exception
                val a: Int < Abort[Exception]                          = Abort.fail(ex)
                val b: Result.Partial[Throwable, Int] < Abort[Nothing] = Abort.runPartial[Throwable](a)
                val c: Result[Nothing, Result.Partial[Throwable, Int]] = Abort.run(b).eval
                assert(c == Result.Success(Result.Failure(ex)))
            }
            "super success" in {
                val a: Int < Abort[Exception]                          = 24
                val b: Result.Partial[Throwable, Int] < Abort[Nothing] = Abort.runPartial[Throwable](a)
                val c: Result[Nothing, Result.Partial[Throwable, Int]] = Abort.run(b).eval
                assert(c == Result.Success(Result.Success(24)))
            }
            "reduce large union incrementally" in {
                val t1: Int < Abort[Int | String | Boolean | Float | Char | Double] =
                    18
                val t2  = Abort.runPartial[Int](t1)
                val t3  = Abort.runPartial[String](t2)
                val t4  = Abort.runPartial[Boolean](t3)
                val t5  = Abort.runPartial[Float](t4)
                val t6  = Abort.runPartial[Char](t5)
                val t7  = Abort.runPartial[Double](t6)
                val res = Abort.run(t7).eval
                assert(res == Result.Success(
                    Result.Success(Result.Success(Result.Success(Result.Success(Result.Success(Result.Success(18))))))
                ))
            }
            "reduce large union in a single expression" in {
                val t: Int < Abort[Int | String | Boolean | Float | Char | Double] = 18
                // NB: Adding type annotation leads to compilation error
                val res =
                    Abort.run:
                        Abort.runPartial[Double](
                            Abort.runPartial[Char](
                                Abort.runPartial[Float](
                                    Abort.runPartial[Boolean](
                                        Abort.runPartial[String](
                                            Abort.runPartial[Int](t)
                                        )
                                    )
                                )
                            )
                        )
                val expected: Result.Partial[
                    Double,
                    Result.Partial[Char, Result.Partial[Float, Result.Partial[Boolean, Result.Partial[String, Result.Partial[Int, Int]]]]]
                ] =
                    Result.Success(Result.Success(Result.Success(Result.Success(Result.Success(Result.Success(18))))))
                assert(res.eval == Result.Success(expected))
            }
            "nested panic to fail" - {
                "catches matching throwable as Failure" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run(Abort.runPartial[RuntimeException](throw ex)).eval
                    assert(result == Result.Success(Result.Failure(ex)))
                }

                "does not convert matching Panic to Failure" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run(Abort.runPartial[RuntimeException](Abort.panic(ex))).eval
                    assert(result == Result.Success(Result.Panic(ex)))
                }

                "doesn't affect Success" in {
                    val result = Abort.run(Abort.runPartial[RuntimeException](42)).eval
                    assert(result == Result.Success(Result.Success(42)))
                }

                "doesn't affect Fail" in {
                    val ex     = new RuntimeException("Test exception")
                    val result = Abort.run(Abort.runPartial[RuntimeException](Abort.fail(ex))).eval
                    assert(result == Result.Success(Result.Failure(ex)))
                }

                "works with nested Aborts" in {
                    val ex = new RuntimeException("Inner exception")
                    val nested = Abort.runPartial[IllegalArgumentException] {
                        Abort.runPartial[RuntimeException](Abort.panic(ex))
                    }
                    val result = Abort.run(nested).eval
                    assert(result == Result.Success(Result.Success(Result.Panic(ex))))
                }
            }
        }
        "fold" - {
            "success" in {
                assert:
                    Abort.run(Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail"
                    )(test(2))).eval == Result.Success("success: 5")

                assert:
                    Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail",
                        (thr) => s"panic: ${thr.getMessage()}"
                    )(test(2)).eval == "success: 5"
            }
            "failure" in {
                assert:
                    Abort.run(Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail"
                    )(test(0))).eval == Result.Success("fail")

                assert:
                    Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail",
                        (thr) => s"panic: ${thr.getMessage()}"
                    )(test(0)).eval == "fail"
            }
            "panic" in {
                val p = new Exception("message")
                assert:
                    Abort.run(Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail"
                    )(Abort.panic(p))).eval == Result.Panic(p)

                assert:
                    Abort.fold[Ex1](
                        (i: Int) => s"success: $i",
                        (ex1) => s"fail",
                        (thr) => s"panic: ${thr.getMessage()}"
                    )(Abort.panic(p)).eval == "panic: message"
            }
        }
        "fail" in {
            val ex: Throwable = new Exception("throwable failure")
            val a             = Abort.fail(ex)
            assert(Abort.run[Throwable](a).eval == Result.fail(ex))
        }
        "fail inferred" in {
            val e = "test"
            val f = Abort.fail(e)
            assert(Abort.run(f).eval == Result.fail(e))
        }
        "error" - {
            "fail" in {
                val ex: Throwable = new Exception("throwable failure")
                val a             = Abort.error(Result.Failure(ex))
                assert(Abort.run[Throwable](a).eval == Result.fail(ex))
            }
            "panic" in {
                val ex: Throwable = new Exception("throwable failure")
                val a             = Abort.error(Result.Panic(ex))
                assert(Abort.run[Throwable](a).eval == Result.panic(ex))
            }
        }
        "when" - {
            "basic usage" in {
                def test(b: Boolean) = Abort.run[String](Abort.when(b)("FAIL!")).eval

                assert(test(true) == Result.fail("FAIL!"))
                assert(test(false) == Result.succeed(()))
            }

            "with Env" in {
                def test(env: Int) = Env.run(env) {
                    Abort.run[String](
                        for
                            x <- Env.get[Int]
                            _ <- Abort.when(x > 5)("Too big")
                            _ <- Abort.when(x < 0)("Negative")
                        yield x
                    )
                }.eval

                assert(test(3) == Result.succeed(3))
                assert(test(7) == Result.fail("Too big"))
                assert(test(-1) == Result.fail("Negative"))
            }

            "with Var" in {
                def test(initial: Int) = Var.run(initial) {
                    Abort.run[String] {
                        for
                            v  <- Var.get[Int]
                            _  <- Abort.when(v % 2 == 0)("Even")
                            _  <- Var.update[Int](_ + 1)
                            v2 <- Var.get[Int]
                            _  <- Abort.when(v2 > 5)("Too big")
                        yield Var.get[Int]
                    }
                }.eval

                assert(test(1) == Result.succeed(2))
                assert(test(2) == Result.fail("Even"))
                assert(test(5) == Result.fail("Too big"))
            }

            "short-circuiting" in {
                var sideEffect = 0
                def test(b: Boolean) = Abort.run[String] {
                    for
                        _ <- Abort.when(b)("FAIL!")
                        _ <- Env.use[Unit](_ => sideEffect += 1)
                    yield ()
                }

                assert(Env.run(())(test(true)).eval == Result.fail("FAIL!"))
                assert(sideEffect == 0)
                assert(Env.run(())(test(false)).eval == Result.succeed(()))
                assert(sideEffect == 1)
            }

            "with S effect in condition" in {
                def test(env: Int) = Env.run(env) {
                    Abort.run[String](
                        Abort.when(Env.use[Int](_ > 5))("Too big")
                    )
                }.eval

                assert(test(3) == Result.succeed(()))
                assert(test(7) == Result.fail("Too big"))
            }

            "with S effect in value" in {
                def test(env: String) = Env.run(env) {
                    Abort.run[String](
                        Abort.when(true)(Env.get[String])
                    )
                }.eval

                assert(test("Error message") == Result.fail("Error message"))
            }
        }

        "unless" - {
            "basic usage" in {
                def test(b: Boolean) = Abort.run[String](Abort.unless(b)("FAIL!")).eval

                assert(test(false) == Result.fail("FAIL!"))
                assert(test(true) == Result.succeed(()))
            }

            "with S effect in condition" in {
                def test(env: Int) = Env.run(env) {
                    Abort.run[String](
                        Abort.unless(Env.use[Int](_ <= 5))("Too big")
                    )
                }.eval

                assert(test(3) == Result.succeed(()))
                assert(test(7) == Result.fail("Too big"))
            }

            "with S effect in value" in {
                def test(env: String) = Env.run(env) {
                    Abort.run[String](
                        Abort.unless(false)(Env.get[String])
                    )
                }.eval

                assert(test("Error message") == Result.fail("Error message"))
            }
        }

        "ensuring" - {
            "basic usage" in {
                def test(x: Int) = Abort.run[String](Abort.ensuring(x > 0, x)("Non-positive")).eval

                assert(test(5) == Result.succeed(5))
                assert(test(0) == Result.fail("Non-positive"))
                assert(test(-3) == Result.fail("Non-positive"))
            }

            "with Env" in {
                def test(env: Int) = Env.run(env) {
                    Abort.run[String] {
                        for
                            x      <- Env.get[Int]
                            result <- Abort.ensuring(x >= 0 && x <= 10, x)("Out of range")
                        yield result * 2
                    }
                }.eval

                assert(test(5) == Result.succeed(10))
                assert(test(0) == Result.succeed(0))
                assert(test(10) == Result.succeed(20))
                assert(test(-1) == Result.fail("Out of range"))
                assert(test(11) == Result.fail("Out of range"))
            }

            "with Var" in {
                def test(initial: Int) = Var.run(initial) {
                    Abort.run[String] {
                        for
                            x      <- Var.get[Int]
                            _      <- Abort.ensuring(x % 2 == 0, ())("Odd")
                            _      <- Var.update[Int](_ * 2)
                            result <- Var.get[Int]
                        yield result
                    }
                }.eval

                assert(test(2) == Result.succeed(4))
                assert(test(4) == Result.succeed(8))
                assert(test(1) == Result.fail("Odd"))
                assert(test(3) == Result.fail("Odd"))
            }

        }
        "catching" - {
            "only effect" - {
                def test(v: Int): Int =
                    v match
                        case 0 => throw ex1
                        case i => 10 / i
                "success" in {
                    assert(
                        Abort.run[Ex1](Abort.catching[Ex1](test(2))).eval ==
                            Result.succeed(5)
                    )
                }
                "failure" in {
                    assert(
                        Abort.run[Ex1](Abort.catching[Ex1](test(0))).eval ==
                            Result.fail(ex1)
                    )
                }
                "subclass" in {
                    assert(
                        Abort.run[RuntimeException](Abort.catching[RuntimeException](test(0))).eval ==
                            Result.fail(ex1)
                    )
                }
                "distinct" in {
                    class Distinct1 extends Throwable derives CanEqual
                    val d1 = new Distinct1
                    class Distinct2 extends Throwable derives CanEqual
                    val d2 = new Distinct2

                    val distinct: Boolean < Abort[Distinct1 | Distinct2] =
                        for
                            _ <- Abort.catching[Distinct1](throw d1)
                            _ <- Abort.catching[Distinct2](throw d2)
                        yield true

                    val a = Abort.run[Distinct1](distinct)
                    val b = Abort.run[Distinct2](a).eval
                    assert(b == Result.succeed(Result.fail(d1)))
                    val c = Abort.run[Distinct1 | Distinct2](distinct).eval
                    assert(c == Result.fail(d1))
                }
                "ClassTag inference" in {
                    val r = Abort.run(Abort.catching(throw new RuntimeException)).eval
                    assert(r.isPanic)
                }
            }
            "with other effect" - {
                def test(v: Int < Env[Int]): Int < Env[Int] =
                    v.map {
                        case 0 =>
                            throw ex1
                        case i =>
                            10 / i
                    }
                "success" in {
                    assert(
                        Env.run(2)(
                            Abort.run[Ex1](Abort.catching[Ex1](test(Env.get)))
                        ).eval ==
                            Result.succeed(5)
                    )
                }
                "failure" in {
                    assert(
                        Env.run(0)(
                            Abort.run[Ex1](Abort.catching[Ex1](test(Env.get)))
                        ).eval ==
                            Result.fail(ex1)
                    )
                }
            }

            "nested Abort effects" - {
                "should propagate the innermost failure" in {
                    val nested = Abort.run[String](
                        Abort.run[Int](
                            Abort.fail[String]("inner").map(_ => Abort.fail[Int](42))
                        )
                    )
                    assert(nested.eval == Result.fail("inner"))
                }

                "should propagate the outermost failure if there are no inner failures" in {
                    val nested = Abort.run(Abort.run[String](
                        Abort.run[Int](Abort.get[Int](Right(42)))
                    ).map(_ => Abort.fail("outer")))
                    assert(nested.eval == Result.fail("outer"))
                }
            }

            "interactions with Env" - {
                "should have access to the environment within Abort" in {
                    val env    = "test"
                    val result = Env.run(env)(Abort.run[String](Env.get[String]))
                    assert(result.eval == Result.succeed(env))
                }

                "should propagate Abort failures within Env" in {
                    val result = Env.run("test")(Abort.run[String](Abort.fail("failure")))
                    assert(result.eval == Result.fail("failure"))
                }
            }

            "interactions with Var" - {
                "should have access to the state within Abort" in {
                    val result = Var.run(42)(
                        Abort.run[String](
                            Var.get[Int].map(_.toString)
                        )
                    )
                    assert(result.eval == Result.succeed("42"))
                }

                "should not modify state on Abort failures" in {
                    val result = Var.run(42)(
                        Abort.run[String](
                            Var.set[Int](24).map(_ => Abort.fail("failure"))
                        )
                    )
                    assert(result.eval == Result.fail("failure"))
                    assert(Var.run(42)(Var.get[Int]).eval == 42)
                }
            }

            "short-circuiting with map" - {
                "should not execute subsequent operations on failure" in {
                    var executed = false
                    val result = Abort.run[String](
                        Abort.fail("failure").map(_ => executed = true)
                    )
                    assert(result.eval == Result.fail("failure"))
                    assert(!executed)
                }

                "should execute subsequent operations on success" in {
                    var executed = false
                    val result = Abort.run(Abort.run[String](
                        Abort.get[Int](Right(42)).map(_ => executed = true)
                    ))
                    assert(result.eval == Result.succeed(Result.succeed(())))
                    assert(executed)
                }
            }

            "handle non-union exceptions as panic" in {
                val result = Abort.run[IllegalArgumentException | NumberFormatException](
                    Abort.catching[IllegalArgumentException | NumberFormatException](throw new RuntimeException)
                ).eval

                assert(result.isInstanceOf[Result.Panic])
            }

            "catch exceptions in nested computations" in {
                def nestedComputation(): Int < Abort[ArithmeticException | IllegalArgumentException] =
                    for
                        _ <- Abort.catching[ArithmeticException](10 / 0)
                        _ <- Abort.catching[IllegalArgumentException](throw new IllegalArgumentException)
                    yield 42

                val result = Abort.run[ArithmeticException | IllegalArgumentException](nestedComputation()).eval

                assert(result.failure.exists {
                    case ex: ArithmeticException => true
                    case _                       => false
                })
            }

            "handle success case with union types" in {
                val result = Abort.run[IllegalArgumentException | NumberFormatException](
                    Abort.catching[IllegalArgumentException | NumberFormatException](42)
                ).eval

                assert(result == Result.succeed(42))
            }
        }
    }

    "interactions with Env and Var" - {
        "nested Abort and Env" in {
            val result = Env.run(5) {
                Abort.run[String] {
                    for
                        x <- Env.get[Int]
                        _ <- if x > 10 then Abort.fail("Too big") else Env.get[Int]
                        y <- Env.use[Int](_ * 2)
                    yield y
                }
            }
            assert(result.eval == Result.succeed(10))
        }

        "Abort failure through Env and Var" in {
            val result = Env.run(15) {
                Var.run(0) {
                    Abort.run[String] {
                        for
                            x <- Env.get[Int]
                            _ <- Var.update[Int](_ + x)
                            _ <- if x > 10 then Abort.fail("Too big") else Var.get[Int]
                        yield ()
                    }
                }
            }
            assert(result.eval == Result.fail("Too big"))
        }
    }

    "edge cases" - {
        "Abort within map" in {
            val result = Abort.run[String] {
                Env.get[Int].map { x =>
                    if x > 5 then Abort.fail("Too big")
                    else Env.get[Int]
                }
            }
            assert(Env.run(10)(result).eval == Result.fail("Too big"))
        }

        "multiple Aborts in for-comprehension" in {
            val result = Abort.run[String] {
                for
                    x <- Env.get[Int]
                    _ <- if x > 5 then Abort.fail("Too big") else Env.get[Int]
                    y <- Var.get[Int]
                    _ <- if y < 0 then Abort.fail("Negative") else Var.get[Int]
                yield (x, y)
            }
            val finalResult = Env.run(3) {
                Var.run(-1)(result)
            }
            assert(finalResult.eval == Result.fail("Negative"))
        }

        "Abort within Abort" in {
            val innerAbort = Abort.run[Int] {
                Abort.fail[String]("Inner error").map(_ => 42)
            }
            val outerAbort = Abort.run[String] {
                innerAbort.map(x => if x.value.exists(_ > 50) then Abort.fail("Outer error") else x)
            }
            assert(outerAbort.eval == Result.fail("Inner error"))
        }

        "deeply nested Aborts" in {
            def nestedAborts(depth: Int): Int < Abort[Int] =
                if depth == 0 then 0
                else Abort.get(Right(depth)).map(_ => nestedAborts(depth - 1))

            val result = Abort.run(nestedAborts(10000))
            assert(result.eval == Result.succeed(0))
        }
    }

    "type inference with multiple effects" in {
        val result = Abort.run[String] {
            for
                x <- Env.get[Int]
                y <- Var.get[Int]
                _ <- if x + y > 10 then Abort.fail("Sum too large") else Env.get[Int]
            yield x + y
        }
        val finalResult: Result[String, Int] < (Env[Int] & Var[Int]) = result
        val _                                                        = finalResult
        succeed
    }

    "handling of Abort[Nothing]" - {
        val ex = new RuntimeException("Panic!")

        "handle Abort[Nothing]" in {
            val computation: Int < Abort[Nothing] = Abort.panic(ex)
            val result                            = Abort.run(computation).eval
            val _: Result[Nothing, Int]           = result
            assert(result == Result.panic(ex))
        }

        "handle Abort[Nothing] with custom error type" in {
            val computation: Int < Abort[Nothing] = Abort.panic(ex)
            val result                            = Abort.run[String](computation).eval
            val _: Result[String, Int]            = result
            assert(result == Result.panic(ex))
        }

        "allow handling of pure values" in {
            val computation: Int < Abort[Nothing] = 42
            val result                            = Abort.run(computation).eval
            val _: Result[Nothing, Int]           = result
            assert(result == Result.succeed(42))
        }

        "work with other effect" in {
            val computation: Int < (Abort[Nothing] & Env[Int]) =
                Env.use[Int](_ => Abort.panic(ex))

            val result =
                Env.run(42)(Abort.run(computation)).eval

            val _: Result[Nothing, Int] = result

            assert(result == Result.panic(ex))
        }
    }

    "Abort.run with parametrized type" in pendingUntilFixed {
        class Test[A]
        typeCheck("Abort.run(Abort.fail(new Test[Int]))")
    }

    "Abort.run with type unions" - {
        case class CustomError(message: String) derives CanEqual

        "handle part of union types" in {
            val computation: Int < Abort[String | Int | CustomError] = Abort.fail("String error")
            val result                                               = Abort.run[String](computation)
            val finalResult                                          = Abort.run[Int | CustomError](result).eval
            assert(finalResult == Result.succeed(Result.fail("String error")))
        }

        "handle multiple types from union" in {
            def test(failWithString: Boolean): Result[String | Int, Int] < Abort[CustomError] =
                val computation: Int < Abort[String | Int | CustomError] =
                    if failWithString then Abort.fail("String error")
                    else Abort.fail(42)

                Abort.run[String | Int](computation)
            end test

            assert(Abort.run[CustomError](test(true)).eval == Result.succeed(Result.fail("String error")))
            assert(Abort.run[CustomError](test(false)).eval == Result.succeed(Result.fail(42)))
        }

        "handle all types from union" in {
            def test(failureType: Int): Result[String | Int | CustomError, Int] =
                val computation: Int < Abort[String | Int | CustomError] =
                    failureType match
                        case 0 => Abort.fail("String error")
                        case 1 => Abort.fail(42)
                        case 2 => Abort.fail(CustomError("Custom error"))

                Abort.run[String | Int | CustomError](computation).eval
            end test

            assert(test(0) == Result.fail("String error"))
            assert(test(1) == Result.fail(42))
            assert(test(2) == Result.fail(CustomError("Custom error")))
        }

        "handle part of union types with success case" in {
            def test(succeed: Boolean): Result[String | Int, Int] < Abort[CustomError] =
                val computation: Int < Abort[String | Int | CustomError] =
                    if succeed then 100
                    else Abort.fail(CustomError("Custom error"))

                Abort.run[String | Int](computation)
            end test

            assert(Abort.run[CustomError](test(true)).eval == Result.succeed(Result.succeed(100)))
            assert(Abort.run[CustomError](test(false)).eval == Result.fail(CustomError("Custom error")))
        }

        "nested handling of union types" in {
            val computation: Int < Abort[String | Int | CustomError | Boolean] =
                Abort.fail("String error")

            val result1 = Abort.run[String](computation)
            val result2 = Abort.run[Int](result1)
            val result3 = Abort.run[CustomError](result2)
            val finalResult: Result[CustomError, Result[Int, Result[String, Int]]] < Abort[Boolean] =
                result3

            assert(Abort.run[Boolean](finalResult).eval == Result.succeed(Result.succeed(Result.succeed(Result.fail("String error")))))
        }
    }

    "Abort.recover" - {
        case class CustomError(message: String) derives CanEqual

        "without onPanic" - {

            "handles expected errors" in {
                val computation = Abort.fail(CustomError("Expected error"))
                val recovered   = Abort.recover[CustomError](_ => 42)(computation)
                assert(Abort.run(recovered).eval == Result.succeed(42))
            }

            "leaves panics unhandled" in {
                val ex          = new RuntimeException("Panic!")
                val computation = Abort.panic(ex)
                val recovered   = Abort.recover[CustomError](_ => 42)(computation)
                assert(Abort.run(recovered).eval == Result.panic(ex))
            }

            "doesn't affect successful computations" in {
                val computation: Int < Abort[CustomError] = 100
                val recovered =
                    Abort.recover[CustomError](_ => 42)(computation)
                assert(Abort.run(recovered).eval == Result.succeed(100))
            }

            "keeps Abort in the effect set for panics" in {
                val ex          = new RuntimeException("Panic!")
                val computation = Abort.panic(ex)
                val recovered   = Abort.recover[CustomError](_ => 42)(computation)
                typeCheckFailure("val _: Int < Any = recovered")("Required: Int < Any")
            }
        }

        "with onPanic" - {

            "handles expected errors" in {
                val computation = Abort.fail(CustomError("Expected error"))
                val recovered   = Abort.recover[CustomError](_ => 42, _ => -1)(computation)
                assert(recovered.eval == 42)
            }

            "handles panics" in {
                val ex          = new RuntimeException("Panic!")
                val computation = Abort.panic(ex)
                val recovered   = Abort.recover[CustomError](_ => 42, _ => -1)(computation)
                assert(recovered.eval == -1)
            }

            "doesn't affect successful computations" in {
                val computation: Int < Abort[CustomError] = 100
                val recovered                             = Abort.recover[CustomError](_ => 42, _ => -1)(computation)
                assert(recovered.eval == 100)
            }

            "removes Abort from the effect set" in {
                val computation = Abort.fail(CustomError("Expected error"))
                val recovered   = Abort.recover[CustomError](_ => 42, _ => -1)(computation)
                typeCheck("val _: Int < Any = recovered")
            }
        }

        "interaction with other effects" - {
            "works with Env" in {
                val computation: Int < (Abort[CustomError] & Env[String]) =
                    for
                        env    <- Env.get[String]
                        result <- if env == "fail" then Abort.fail(CustomError("Failed")) else Kyo.lift(env.length)
                    yield result

                val recovered = Abort.recover[CustomError](_ => -1)(computation)
                val result    = Env.run("success")(Abort.run(recovered))
                assert(result.eval == Result.succeed(7))

                val failResult = Env.run("fail")(Abort.run(recovered))
                assert(failResult.eval == Result.succeed(-1))
            }

            "works with Var" in {
                val computation: Int < (Abort[CustomError] & Var[Int]) =
                    for
                        value  <- Var.get[Int]
                        result <- if value > 10 then Abort.fail(CustomError("Too large")) else Kyo.lift(value)
                    yield result

                val recovered = Abort.recover[CustomError](_ => -1)(computation)
                val result    = Var.run(5)(Abort.run(recovered))
                assert(result.eval == Result.succeed(5))

                val failResult = Var.run(15)(Abort.run(recovered))
                assert(failResult.eval == Result.succeed(-1))
            }
        }

        "recover function using other effects" - {
            case class CustomError(message: String) derives CanEqual

            "with Env effect" in {
                val computation: Int < Abort[CustomError] = Abort.fail(CustomError("Failed"))
                val recovered = Abort.recover[CustomError] { error =>
                    Env.get[String].map(_.length)
                }(computation)

                val result = Env.run("TestEnv")(Abort.run(recovered))
                assert(result.eval == Result.succeed(7))
            }

            "with Var effect" in {
                val computation: Int < Abort[CustomError] = Abort.fail(CustomError("Failed"))
                val recovered = Abort.recover[CustomError] { error =>
                    for
                        current <- Var.get[Int]
                        _       <- Var.set(current + error.message.length)
                        result  <- Var.get[Int]
                    yield result
                }(computation)

                val result = Var.run(10)(Abort.run(recovered))
                assert(result.eval == Result.succeed(16))
            }

            "with both Env and Var effects" in {
                val computation: Int < Abort[CustomError] = Abort.fail(CustomError("Error"))
                val recovered = Abort.recover[CustomError] { error =>
                    for
                        env    <- Env.get[String]
                        _      <- Var.update[Int](_ + env.length + error.message.length)
                        result <- Var.get[Int]
                    yield result
                }(computation)

                val result = Env.run("TestEnv")(Var.run(5)(Abort.run(recovered)))
                assert(result.eval == Result.succeed(17))
            }

            "with nested Abort effect" in {
                val computation = Abort.fail(CustomError("Outer"))
                val recovered   = Abort.recover[CustomError](_ => Abort.fail("Inner"))(computation)

                assert(Abort.run(recovered).eval == Result.fail("Inner"))
            }

            "with onPanic using effects" in {
                val ex                                    = new RuntimeException("Panic!")
                val computation: Int < Abort[CustomError] = Abort.panic(ex)
                val recovered = Abort.recover[CustomError](
                    onFail = _ => Env.get[Int],
                    onPanic = _ => Var.update[Int](_ + 1).andThen(Var.get[Int])
                )(computation)

                val result = Env.run(42)(Var.run(10)(recovered))
                assert(result.eval == 11)
            }
        }

        "with handle operator" - {
            case class CustomError(message: String) derives CanEqual

            "recovers from failures" in {
                val computation: Int < Abort[CustomError] = Abort.fail(CustomError("Failed"))
                val result                                = computation.handle(Abort.recover[CustomError](_ => 42))
                assert(Abort.run(result).eval == Result.succeed(42))
            }

            "doesn't affect successful computations" in {
                val computation: Int < Abort[CustomError] = 10
                val result                                = computation.handle(Abort.recover[CustomError](_ => 42))
                assert(Abort.run(result).eval == Result.succeed(10))
            }

            "can be chained with other operations" in {
                val computation: Int < Abort[CustomError] = Abort.fail(CustomError("Failed"))
                val result = computation
                    .handle(Abort.recover[CustomError](_ => 42))
                    .map(_ * 2)

                assert(Abort.run(result).eval == Result.succeed(84))
            }

            "works with onPanic" in {
                val ex                                    = new RuntimeException("Panic!")
                val computation: Int < Abort[CustomError] = Abort.panic(ex)
                val result = computation.handle(Abort.recover[CustomError](
                    onFail = _ => 42,
                    onPanic = _ => -1
                ))
                assert(result.eval == -1)
            }
        }
    }

    "literal" - {
        "string" in {
            val result                      = Abort.literal.fail("FAIL!")
            val _: Nothing < Abort["FAIL!"] = result
            val _: Nothing < Abort[String]  = result
            assert(Abort.run(result).eval == Result.fail("FAIL!"))
        }

        "numeric" in {
            val result                 = Abort.literal.fail(-1)
            val _: Nothing < Abort[-1] = result
            assert(Abort.run(result).eval == Result.fail(-1))
        }

        "ensuring" in {
            def divide(a: Int, b: Int): Int < Abort["Division by zero"] =
                Abort.literal.ensuring(b != 0, a / b)("Division by zero")

            val result                             = divide(1, 0)
            val _: Int < Abort["Division by zero"] = result
            assert(Abort.run(result).eval == Result.fail("Division by zero"))
            assert(Abort.run(divide(1, 1)).eval == Result.succeed(1))
        }

        "unless" in {
            def unless(b: Boolean): Unit < Abort["BOOM"] =
                Abort.literal.unless(b)("BOOM")

            val result                  = unless(false)
            val _: Unit < Abort["BOOM"] = result
            assert(Abort.run(result).eval == Result.fail("BOOM"))
            assert(Abort.run(unless(true)).eval == Result.unit)
        }

        "when" in {
            def when(b: Boolean): Unit < Abort["TOO_BIG"] =
                Abort.literal.when(b)("TOO_BIG")

            val result                     = when(false)
            val _: Unit < Abort["TOO_BIG"] = result
            assert(Abort.run(result).eval == Result.unit)
            assert(Abort.run(when(true)).eval == Result.fail("TOO_BIG"))
        }

        "effects" in {
            val result = Env.run(15) {
                Abort.literal.run {
                    for
                        x <- Env.get[Int]
                        _ <- Abort.literal.when(x > 10)("TOO_BIG")
                    yield x
                }
            }.eval
            val _: Result["TOO_BIG", Int] = result
            assert(result == Result.fail("TOO_BIG"))
        }

        "generic" in {
            def test[A](a: A): Nothing < Abort[A] =
                Abort.literal.fail(a)

            val result                  = test(1)
            val _: Nothing < Abort[Int] = result
            assert(Abort.run(result).eval == Result.fail(1))
        }

        "union" in {
            val result                                 = Abort.literal.fail("FAIL!")
            val _: Nothing < Abort["FAIL!" | "FAIL2!"] = result
            assert(Abort.run(result).eval == Result.fail("FAIL!"))
        }
    }

    "effect nesting" - {
        "basic nesting with flatten and recovery" in {
            val success = Kyo.lift(5: Int < Abort[String])
            val failure = Kyo.lift(Abort.fail("outer failure"))

            val comp = Abort.run(success.flatten).map(_.foldError(_ == 5, _ => false))
            assert(comp.eval)

            val comp2 = Abort.run(failure.flatten).map(_.foldError(_ => false, _ == Result.fail("outer failure")))
            assert(comp2.eval)
        }

        "multi-level" in {

            val deep   = Kyo.lift(Abort.fail(42))
            val result = Abort.run[Int](Abort.run[String](deep.flatten.flatten))
            assert(result.eval.foldError(_ => false, _ == Result.fail(42)))

            val stringError = Kyo.lift(Abort.fail("error"): Int < Abort[String])
            val intError    = Kyo.lift(Abort.fail(404): Int < Abort[Int])

            assert(Abort.run[String](stringError.flatten).eval.foldError(_ => false, _ == Result.fail("error")))
            assert(Abort.run[Int](intError.flatten).eval.foldError(_ => false, _ == Result.fail(404)))
        }

        "with other effects" in {
            val local = Local.init("default")
            val combined = Kyo.lift {
                local.let("custom") {
                    Choice.eval(1, 2).flatMap { n =>
                        if n % 2 == 0 then n * 10
                        else Abort.fail(s"odd: $n")
                    }
                }
            }

            val results = Choice.run(Abort.run(combined.flatten)).eval
            assert(results.length == 2)
            assert(results(0) == Result.fail("odd: 1"))
            assert(results(1) == Result.succeed(20))
        }
    }

    "recoverError" - {
        case class CustomError(message: String) derives CanEqual

        "handles failures" in {
            val computation = Abort.fail(CustomError("Expected error"))
            val recovered =
                Abort.recoverError[CustomError] {
                    error => s"Recovered: ${error.show}"
                }(computation)

            assert(Abort.run(recovered).eval == Result.succeed("Recovered: Failure(CustomError(Expected error))"))
        }

        "handles panics" in {
            val ex          = new RuntimeException("Panic message")
            val computation = Abort.panic(ex)
            val recovered = Abort.recoverError[CustomError] {
                error => s"Recovered: ${error.show}"
            }(computation)

            assert(Abort.run(recovered).eval == Result.succeed("Recovered: Panic(java.lang.RuntimeException: Panic message)"))
        }

        "doesn't affect successful computations" in {
            val computation: String < Abort[CustomError] = "success"
            var called                                   = false
            val recovered =
                Abort.recoverError[CustomError] { _ =>
                    called = true
                    "Should not be called"
                }(computation)

            assert(called == false)
            assert(Abort.run(recovered).eval == Result.succeed("success"))
        }
    }

    "foldError" - {
        case class CustomError(message: String) derives CanEqual

        "handles success case" in {
            val computation: Int < Abort[CustomError] = 42
            val result = Abort.foldError[CustomError](
                onSuccess = i => s"Success: $i",
                onError = error => s"Error: ${error.show}"
            )(computation)

            assert(result.eval == "Success: 42")
        }

        "handles failure case" in {
            val computation = Abort.fail(CustomError("Expected error"))
            val result = Abort.foldError[CustomError](
                onSuccess = i => s"Success: $i",
                onError = error => s"Error: ${error.show}"
            )(computation)

            assert(result.eval == "Error: Failure(CustomError(Expected error))")
        }

        "handles panic case" in {
            val ex          = new RuntimeException("Panic message")
            val computation = Abort.panic(ex)
            val result = Abort.foldError[CustomError](
                onSuccess = i => s"Success: $i",
                onError = error => s"Error: ${error.show}"
            )(computation)

            assert(result.eval == "Error: Panic(java.lang.RuntimeException: Panic message)")
        }

        "removes Abort from the effect set" in {
            val computation = Abort.fail(CustomError("Expected error"))
            val folded = Abort.foldError[CustomError](
                onSuccess = i => s"Success: $i",
                onError = _ => "Error handled"
            )(computation)

            val _: String < Any = folded
            succeed
        }
    }

end AbortTest
