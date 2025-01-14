---
layout: home
title:  "Home"
section: "home"
position: 1
---

Difflicious is a library that compare and calculates differences between two objects. 

[![Release](https://img.shields.io/nexus/r/com.github.jatcwang/difflicious-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/releases/com/github/jatcwang/difflicious-core_2.13/)
[![(https://badges.gitter.im/gitterHQ/gitter.png)](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jatcwang/difflicious)

- Readable and Actionable diff results
- Flexible & Configurable diffing logic
  - Ignore unimportant fields when comparing
  - Compare `List`s of items independent of order
  - Match `Map` entries by key and show diffs of the values
  - Change comparison logic at run time!
- Integration with test frameworks and libraries  

# Installation

If you're using the [MUnit](https://scalameta.org/munit/) test framework:
```
// == SBT ==
"com.github.jatcwang" %% "difflicious-munit" % "{{ site.version }}" 
// == Mill == 
ivy"com.github.jatcwang::difflicious-munit:{{ site.version }}"
```

If you're using [ScalaTest](https://www.scalatest.org/) test framework:
```
// == SBT ==
"com.github.jatcwang" %% "difflicious-scalatest" % "{{ site.version }}" 
// == Mill == 
ivy"com.github.jatcwang::difflicious-scalatest:{{ site.version }}"
```
