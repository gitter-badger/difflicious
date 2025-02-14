---
layout: docs
title:  "Introduction"
permalink: docs/introduction
---

# Introduction

**Difflicious** is a library to help you diff objects in tests. It aims to:

* **Readable** and **Actionable** diffs
* **Customizability**: supporting all kinds of tweaks you'd want to do such as ignoring fields or compare lists independent of
  element order.
  
Here's a motivational example!

```scala mdoc:silent
import difflicious._
import difflicious.implicits._

sealed trait HousePet {
  def name: String
}
object HousePet {
  final case class Dog(name: String, age: Int) extends HousePet
  final case class Cat(name: String, livesLeft: Int) extends HousePet
  
  implicit val differ: Differ[HousePet] = Differ.derive
}

import HousePet.{Cat, Dog}

val petsDiffer = Differ[List[HousePet]]
  .pairBy(_.name)                          // Match pets in the list by name for comparison
  .ignoreAt(_.each.subType[Cat].livesLeft) // Don't worry about livesLeft for cats when comparing
  
petsDiffer.diff(
  obtained = List(
    Dog("Andy", 12),
    Cat("Dr.Evil", 8),
    Dog("Lucky", 5),
  ),
  expected = List(
    Dog("Lucky", 6),
    Cat("Dr.Evil", 9),
    Cat("Andy", 12),
  )
)
```

And this is the diffs you will see:

<pre class="diff-render">
List(
  <span style="color: red;">Dog</span> != <span style="color: green;">Cat</span>
  <span style="color: red;">=== Obtained ===
  Dog(
    name: "Andy",
    age: 12,
  )</span>
  <span style="color: green;">=== Expected ===
  Catmenu.yml(
    name: "Andy",
    livesLeft: [IGNORED],
  )</span>,
  Cat(
    name: "Dr.Evil",
    livesLeft: <span style="color: gray;">[IGNORED]</span>,
  ),
  Dog(
    name: "Lucky",
    age: <span style="color: red;">5</span> -> <span style="color: green;">6</span>,
  ),
)
</pre>

In the example above, difflicious helped us spot:

* That `Andy` is not the cat we expected. (Got a dog instead!)
* The cat `Dr.Evil` is the "same" on both sides, because we decided to not check how many lives 
  the cats have left (Cats love their privacy).
* `Lucky`'s age is wrong.
