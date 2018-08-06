package eie

trait ByteFormat[T] extends ToBytes[T] with FromBytes[T]
