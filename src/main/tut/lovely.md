<div style="border-radius: 10px; background: #EEEEEE; padding: 20px; text-align: center; font-size: 1.5em">
  <big><b>Lovely for-comprehensions</b></big> </br>
  </br>
  Adam Rosien <br/>
  <small>CrowdStrike</small> <br/>
  <code>adam@rosien.net</code> <br/>
  <br/>
  <code>@arosien #scalaio</code>
</div>

---

# ![flatMap that shit!](images/flatmap.jpg)

---

# ![y u no use for-comprehension?](images/yuno.jpg)

---

# Types are wonderful...

    def transitionHaveFsPostOpenSnapshotState(
      input: InputMessage,
      tags: CorrelatorTagMap):
        (CorrelatorData,
          (ActorRef, CorrelatorIndex) => Unit)

---

# ... but they hide the implementation

<img src="images/badimpl.png" height="500"/>

---

# Niiiiiice

```
for {
  response <- searchApi(RuleQuery(search)).run
  alert    <- response.fold(toFailure, toAlert)
} yield alert
```

![thumbs up](images/thumbsup.gif)

---

# `F[_]`

---

# `F[_]`

`F[_]`? What's an `F[_]`?

    import language.higherKinds

    def doStuff[F[_], A](fa: F[A]) = ???

---

# `F[_]`

    package scala.collection
    package immutable

    sealed abstract class List[+A] { // ... }
                             ^ ^
                             ^ ^
                             F[_]

---

# `F[_]`

`Option` is an `F[_]`:

    package scala

    sealed abstract class Option[+A] { // ... }
                               ^ ^
                               ^ ^
                               F[_]

---

# `F[_]`

`F[_]`!!

* `List`
* `Option`
* `Future`
* `Either`?

Lots more....

---

# for-comprehensions

---

# `F[_] => F[_]`

```scala
for {
  s <- Option("stuff")
} yield s.length

for {
  x <- List(1, 2, 3)
  y <- List(4, 5, x)
} yield y + 1
```

---

# As syntactic sugar

A single expression without `yield` translates to `foreach`:

```scala
reflect.runtime.universe.reify {
  for {
    x <- Some(12)
  } println(x)
}.tree
```

---

# As syntactic sugar

A single expression translates to `map`:

```scala
reflect.runtime.universe.reify {
  for {
    x <- Some(12)
  } yield x + 1
}.tree
```

---

# As syntactic sugar

Multiple expressions translate to `flatMap`, with the last one translated to `map`:

```scala
reflect.runtime.universe.reify {
  for {
    x <- Some(12)
    y <- Some(2)
  } yield x * y
}.tree
```

---

# As syntactic sugar

Conditionals are translated to `filter` or `withFilter`:

```scala
reflect.runtime.universe.reify {
  for {
    x <- Some(12) if x < 10
  } yield x + 1
}.tree
```

---

# As syntactic sugar

Intermediate values are translated to two `map`s with a tuple:

```scala
reflect.runtime.universe.reify {
  for {
    x <- Some(12)
    y = x + 1
  } yield y
}.tree
```

---

# Rules & Techniques

---

# Avoid "complexity-braces"

```scala
def uggslies = {
  val thing = ???
  val anotherThingThatDoesStuff = ???

  // DANGER COMPLEX STUFFZ!!

  ???
}
```

"complexity-braces" are a code smell. Braces contain statements with arbitrary side-effects.

---

# Avoid "complexity-braces"

Expressions don't need them. Expressions produce _values_.

```
def thePrecious: MagicEffect[Baz] =
  for {
    foo <- magicFoo
    bar <- magicBar(foo)
  } baz(bar)
}
```

---

# RHS must have same shape

```scala
illTyped("""
  for {
    x <- Some(12)   // Option[Int]
    y <- List(2, 4) // List[Int]
  } yield x * y
""")
```

Danger! `scala.Predef` imports confusing implicit conversions like `Option => Iterable`!

---

# RHS must have same shape

```scala
for {
  x <- Some(12).toList
  y <- List(2, 4)
} yield x * y
```

---

# `traverse`

    F[A]    => (A => G[B])      => G[F[B]]

    e.g.,

    List[A] => (A => Future[B]) => Future[List[B]]

---

# `traverse`

```scala
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

illTyped("""
val xs = List(1, 2, 3)
def doWork(x: Int): Future[Int] =
  Future.successful(x)

val work: Future[List[Int]] =
  for {
    x <- xs        // List[Int]
    y <- doWork(x) // Future[Int]
  } yield y
""")
```

---

# `traverse`

```scala
val xs = List(1, 2, 3)
def doWork(x: Int): Future[Int] =
  Future.successful(x)

val work: Future[List[Int]] =
  Future.traverse(xs)(doWork)

work.value
```

---

# `sequence`

    F[G[A]] => G[F[A]]

    e.g.,

    List[Future[A]] => Future[List[A]]

---

# Avoid `match`

`match`, aka deconstruction, is less powerful than monadic functions, aka for-comprehensions:

```scala
case class Sup(stuff: List[String])
case class Bar(sup: Option[Sup])
case class Foo(bar: Option[Bar])

def zibby(z: Option[Foo]): String =
  z match {
    case Some(foo) =>
      foo.bar match {
        case Some(bar) => if (bar.sup.isDefined) "whee" else "boo"
        case None => ""
      }
    case None => ""
  }
```

---

# Avoid `match`

```scala
def zibby2(z: Option[Foo]): Option[String] =
  for {
    foo <- z
    bar <- foo.bar
  } yield if (bar.sup.isDefined) "whee" else "boo"

val sup = Sup("ick" :: "yuck" :: Nil)
val bar = Bar(Some(sup))
val foo = Foo(Some(bar))

zibby2(Some(foo))
```

---

# Inline value declarations

```scala
val classifications = Map.empty[String, Map[String, String]]

val sha256Classifications = classifications.get("msg.sha256")
val customerResult = sha256Classifications.flatMap(_.get("msg.cid"))
val globalResult = sha256Classifications.flatMap(_.get("GlobalColumnName"))
val finalResult: Option[String] = customerResult orElse globalResult
```

---

# Inline value declarations

```scala
val finalResult2: Option[String] =
  for {
    sha256Classifications <- classifications.get("msg.sha256")
    cid = sha256Classifications.get("msg.cid")
    global = sha256Classifications.get("GlobalColumnName")
    r <- cid orElse global
  } yield r
```

---

# Abstracting over `F[_]`

```scala
for {
  i <- Option(1)
} yield i + 1

for {
  i <- List(1, 2, 3)
} yield i + 1

for {
  i <- Future.successful(1)
} yield i + 1
```

---

# Abstracting over `F[_]`

```scala
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import scalaz.contrib.std._ // only needed for scalaz 7.0.x

def plusOne[F[_] : Functor](fi: F[Int]): F[Int] =
  for {
    i <- fi
  } yield i + 1

plusOne(Option(1))
plusOne(List(1, 2, 3))
Await.result(
  plusOne(Future.successful(1)),
  1.second)
```

---

# Abstracting over `F[_]`

```scala
val plusOne2: Int => Int = _ + 1
val optionPlusOne: Option[Int] => Option[Int] =
  Functor[Option].lift(plusOne2)

optionPlusOne(Some(41))
```

We can separate our pure functions from our effects!

---

# In summary

---

# In summary

for-comprehension = just good refactoring

* Know your desugaring
* Avoid complexity-braces, `match`
* Use inline values to clean up your for-comps
* Ugly code can be improved!

Thank you!

Adam Rosien / `adam@rosien.net` / `@arosien #scalaio`