# EIE

![sbt test](https://travis-ci.org/aaronp/eie.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/eie/badge.svg?branch=master)](https://coveralls.io/github/aaronp/eie?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/eie_2.13/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/eie_2.13)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/eie_2.13.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/eie_2.13)


'EIE' is yet another IO project, written in scala. It's inspiration is
from utilities in e.g. apache-commons and better-files (both good libraries).

It's mostly useful for pimping java.nio.Path, and providing the ToBytes and FromBytes
type-classes, and exposing some other basic DAO operations on a file system.


You can read more about the design and check the documentation [here](https://aaronp.github.io/eie)

## Usage

```
libraryDependencies += "com.github.aaronp" %% "eie" % "0.0.3"
```

in maven:

```
<dependency>
  <groupId>com.github.aaronp</groupId>
  <artifactId>eie_2.12</artifactId>
  <version>0.0.3</version>
</dependency>
```


And then:

```
  import eie.io._

  val file: Path = "my/file.txt".asPath.text = "hello world!"

  val contents: String = file.text

  file.delete() // should return 'true'
```

## Building

You can build/test/etc using the usual suspects:

```
sbt clean coveraget test coverageReport doc
```