package difflicious

import difflicious.ConfigureError.{TypeTagMismatch, NonExistentField}
import difflicious.testutils._
import difflicious.testtypes._
import difflicious.implicits._
import izumi.reflect.macrortti.LTag

// Tests for configuring a Differ
class DifferConfigureSpec extends munit.FunSuite {

  test("Differ#ignore works") {
    assertConsoleDiffOutput(
      CC.differ.ignore,
      CC(1, "s", 1.0),
      CC(1, "s", 2.0),
      grayIgnoredStr,
    )
  }

  test("Differ#unignore works") {
    assertConsoleDiffOutput(
      CC.differ.ignore.unignore,
      CC(1, "s", 1.0),
      CC(1, "s", 2.0),
      s"""CC(
         |  i: 1,
         |  s: "s",
         |  dd: ${R}1.0$X -> ${G}2.0$X,
         |)""".stripMargin,
    )
  }

  test("configure path's subType call errors when super type isn't sealed") {
    val compileError = compileErrors("Differ[List[OpenSuperType]].ignoreAt(_.each.subType[OpenSub])")
    val firstLine = compileError.linesIterator.toList.drop(1).head
    assertEquals(firstLine, "Specified subtype is not a known direct subtype of trait OpenSuperType.")
  }

  test("configure path's subType handles multi-level hierarchies") {
    assertConsoleDiffOutput(
      Differ[List[Sealed]].ignoreAt(_.each.subType[SubSealed.SubSub1].d),
      List(
        SubSealed.SubSub1(1.0),
      ),
      List(
        SubSealed.SubSub1(2.0),
      ),
      s"""List(
        |  SubSub1(
        |    d: $grayIgnoredStr,
        |  ),
        |)""".stripMargin,
    )
  }

  test("configure path allows 'each' to resolve underlying differ in a Map") {
    assertConsoleDiffOutput(
      Differ[Map[String, CC]].ignoreAt(_.each.dd),
      Map(
        "a" -> CC(1, "s", 1.0),
      ),
      Map(
        "a" -> CC(1, "s", 2.0),
      ),
      s"""Map(
        |  "a" -> CC(
        |      i: 1,
        |      s: "s",
        |      dd: $I[IGNORED]$X,
        |    ),
        |)""".stripMargin,
    )
  }
  test("configure path allows 'each' to resolve underlying differ in a Seq") {
    assertConsoleDiffOutput(
      Differ[List[CC]].ignoreAt(_.each.dd),
      List(
        CC(1, "s", 1.0),
      ),
      List(
        CC(1, "s", 2.0),
      ),
      s"""List(
       |  CC(
       |    i: 1,
       |    s: "s",
       |    dd: $I[IGNORED]$X,
       |  ),
       |)""".stripMargin,
    )
  }

  test("configure path allows 'each' to resolve underlying differ in a Set") {
    assertConsoleDiffOutput(
      Differ[Set[CC]].pairBy(_.i).ignoreAt(_.each.dd),
      Set(
        CC(1, "s", 1.0),
      ),
      Set(
        CC(1, "s", 2.0),
      ),
      s"""Set(
       |  CC(
       |    i: 1,
       |    s: "s",
       |    dd: $I[IGNORED]$X,
       |  ),
       |)""".stripMargin,
    )
  }

  test("configure path can handle escaped sub-type and field names") {
    assertConsoleDiffOutput(
      Differ[List[Sealed]].ignoreAt(_.each.subType[`Weird@Sub`].`weird@Field`),
      List(
        `Weird@Sub`(1, "a"),
      ),
      List(
        `Weird@Sub`(1, "x"),
      ),
      s"""List(
         |  Weird@Sub(
         |    i: 1,
         |    weird@Field: $I[IGNORED]$X,
         |  ),
         |)""".stripMargin,
    )
  }

  test("pairBy works with Seq") {
    assertConsoleDiffOutput(
      Differ[HasASeq[CC]].configure(_.seq)(_.pairBy(_.i)),
      HasASeq(
        Seq(
          CC(1, "s", 1.0),
          CC(2, "s", 2.0),
        ),
      ),
      HasASeq(
        Seq(
          CC(2, "s", 4.0),
          CC(1, "s", 2.0),
        ),
      ),
      s"""HasASeq(
         |  seq: Seq(
         |    CC(
         |      i: 1,
         |      s: "s",
         |      dd: ${R}1.0$X -> ${G}2.0$X,
         |    ),
         |    CC(
         |      i: 2,
         |      s: "s",
         |      dd: ${R}2.0$X -> ${G}4.0$X,
         |    ),
         |  ),
         |)""".stripMargin,
    )
  }

  test("pairBy works with Set") {
    assertConsoleDiffOutput(
      Differ[Map[String, Set[CC]]].configure(x => x.each)(_.pairBy(_.i)),
      Map(
        "a" -> Set(
          CC(1, "s", 1.0),
          CC(2, "s", 2.0),
        ),
      ),
      Map(
        "a" -> Set(
          CC(2, "s", 4.0),
          CC(1, "s", 2.0),
        ),
      ),
      s"""Map(
         |  "a" -> Set(
         |      CC(
         |        i: 1,
         |        s: "s",
         |        dd: ${R}1.0$X -> ${G}2.0$X,
         |      ),
         |      CC(
         |        i: 2,
         |        s: "s",
         |        dd: ${R}2.0$X -> ${G}4.0$X,
         |      ),
         |    ),
         |)""".stripMargin,
    )
  }

  test("'replace' for MapDiffer replaces value differ when step is 'each'") {
    val differ: Differ[Map[String, CC]] = Differ[Map[String, CC]].configure(_.each)(_.ignore)
    val differWithReplace = differ.replace[CC](_.each)(CC.differ)

    assertConsoleDiffOutput(
      differ,
      Map(
        "a" -> CC(1, "s", 1.0),
      ),
      Map(
        "a" -> CC(1, "s", 4.0),
      ),
      s"""Map(
         |  "a" -> $grayIgnoredStr,
         |)""".stripMargin,
    )

    assertConsoleDiffOutput(
      differWithReplace,
      Map(
        "a" -> CC(1, "s", 1.0),
      ),
      Map(
        "a" -> CC(1, "s", 4.0),
      ),
      s"""Map(
         |  "a" -> CC(
         |      i: 1,
         |      s: "s",
         |      dd: ${R}1.0$X -> ${G}4.0$X,
         |    ),
         |)""".stripMargin,
    )
  }

  test("'replace' for MapDiffer fails if step isn't 'each'") {
    assertEquals(
      Differ[Map[String, CC]]
        .configureRaw(ConfigurePath.of("nope"), ConfigureOp.TransformDiffer[CC](_ => CC.differ, LTag[CC])),
      Left(NonExistentField(configurePathResolved("nope"), "MapDiffer")),
    )
  }

  test("'replace' for MapDiffer fails if type tag mismatches") {
    assertEquals(
      Differ[Map[String, CC]]
        .configureRaw(ConfigurePath.of("each"), ConfigureOp.TransformDiffer[Sealed](_ => Sealed.differ, LTag[Sealed])),
      Left(
        TypeTagMismatch(
          path = configurePathResolved("each"),
          obtainedTag = LTag[Sealed].tag,
          expectedTag = LTag[CC].tag,
        ),
      ),
    )
  }

  test("'replace' for SeqDiffer replaces ite differ when step is 'each'") {
    val differ: Differ[Seq[CC]] = Differ[Seq[CC]].configure(_.each)(_.ignore)
    val differWithReplace = differ.replace[CC](_.each)(CC.differ)

    assertConsoleDiffOutput(
      differ,
      Seq(
        CC(1, "s", 1.0),
      ),
      Seq(
        CC(1, "s", 4.0),
      ),
      s"""Seq(
         |  $grayIgnoredStr,
         |)""".stripMargin,
    )

    assertConsoleDiffOutput(
      differWithReplace,
      Seq(
        CC(1, "s", 1.0),
      ),
      Seq(
        CC(1, "s", 4.0),
      ),
      s"""Seq(
         |  CC(
         |    i: 1,
         |    s: "s",
         |    dd: ${R}1.0$X -> ${G}4.0$X,
         |  ),
         |)""".stripMargin,
    )
  }

  test("'replace' for SeqDiffer fails if step isn't 'each'") {
    assertEquals(
      Differ[Seq[CC]]
        .configureRaw(ConfigurePath.of("nope"), ConfigureOp.TransformDiffer[CC](_ => CC.differ, LTag[CC])),
      Left(NonExistentField(configurePathResolved("nope"), "SeqDiffer")),
    )
  }

  test("'replace' for SeqDiffer fails if type tag mismatches") {
    assertEquals(
      Differ[Seq[CC]]
        .configureRaw(ConfigurePath.of("each"), ConfigureOp.TransformDiffer[Sealed](_ => Sealed.differ, LTag[Sealed])),
      Left(
        TypeTagMismatch(
          path = configurePathResolved("each"),
          obtainedTag = LTag[Sealed].tag,
          expectedTag = LTag[CC].tag,
        ),
      ),
    )
  }

  test("'replace' for SetDiffer replaces ite differ when step is 'each'") {
    val differ: Differ[Set[CC]] = Differ[Set[CC]].configure(_.each)(_.ignore)
    val differWithReplace = differ.replace[CC](_.each)(CC.differ)

    assertConsoleDiffOutput(
      differ,
      Set(
        CC(1, "s", 1.0),
      ),
      Set(
        CC(1, "s", 1.0),
      ),
      s"""Set(
         |  $grayIgnoredStr,
         |)""".stripMargin,
    )

    assertConsoleDiffOutput(
      differWithReplace,
      Set(
        CC(1, "s", 1.0),
      ),
      Set(
        CC(1, "s", 1.0),
      ),
      s"""Set(
         |  CC(
         |    i: 1,
         |    s: "s",
         |    dd: 1.0,
         |  ),
         |)""".stripMargin,
    )
  }

  test("'replace' for SetDiffer fails if type tag mismatches") {
    assertEquals(
      Differ[Set[CC]]
        .configureRaw(ConfigurePath.of("nope"), ConfigureOp.TransformDiffer[CC](_ => CC.differ, LTag[CC])),
      Left(NonExistentField(configurePathResolved("nope"), "SetDiffer")),
    )
  }

  test("'replace' for SeqDiffer fails if step isn't 'each'") {
    assertEquals(
      Differ[Set[CC]]
        .configureRaw(ConfigurePath.of("each"), ConfigureOp.TransformDiffer[Sealed](_ => Sealed.differ, LTag[Sealed])),
      Left(
        TypeTagMismatch(
          path = configurePathResolved("each"),
          obtainedTag = LTag[Sealed].tag,
          expectedTag = LTag[CC].tag,
        ),
      ),
    )
  }

  private def configurePathResolved(path: String*): ConfigurePath = {
    ConfigurePath(path.toVector, List.empty)
  }
}
