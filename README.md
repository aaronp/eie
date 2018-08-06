# EIE

![sbt test cucumber](https://travis-ci.org/aaronp/eie.svg?branch=master)

'EIE' is yet another IO project, written in scala. It's inspiration is
from utilities in e.g. apache-commons and better-files (both good libraries).

It's mostly useful for pimping java.nio.Path, and providing the ToBytes and FromBytes
type-classes, and exposing some other basic DAO operations on a file system.


You can read more about the design and check the documentation [here](https://aaronp.github.io/eie)

## Building

You can build/test/etc using the usual suspects:

```
sbt clean coveraget test coverageReport doc
```