# EIE

![sbt test](https://github.com/aaronp/eie/actions/workflows/scala.yml/badge.svg)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/eie/badge.svg?branch=master)](https://coveralls.io/github/aaronp/eie?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/eie_3.0.0-RC1/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/eie_3.0.0-RC1)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/eie_3.0.0-RC1.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/eie_3.0.0-RC1)


'EIE' is yet another IO project, written in scala. It's inspiration is
from utilities in e.g. apache-commons and better-files (both good libraries).

It's mostly useful for pimping java.nio.Path, and providing the ToBytes and FromBytes
type-classes, and exposing some other basic DAO operations on a file system.


You can read more about the design and check the documentation [here](https://aaronp.github.io/eie)

## Usage


```
  import eie.io._

  val file: java.nio.Path = "my/file.txt".asPath.text = "hello world!"

  val contents: String = file.text

  file.delete()
```

## Building

You can build/test/etc using the usual suspects:

```
sbt clean coverage test coverageReport doc
```
