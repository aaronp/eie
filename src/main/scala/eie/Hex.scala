package eie

object Hex {
  private val hexArray: Array[Char] = "0123456789ABCDEF".toCharArray

  def apply(data: Array[Byte], toDigits: Array[Char] = hexArray) = new String(toChar(data))

  def toChar(data: Array[Byte], toDigits: Array[Char] = hexArray): Array[Char] = {
    val len    = data.length << 1
    val result = new Array[Char](len)
    (0 until len by 2).zipWithIndex.map {
      case (writeIndex, readIndex) =>
        result(writeIndex) = toDigits((0xF0 & data(readIndex)) >>> 4)
        result(writeIndex + 1) = toDigits(0x0F & data(readIndex))
    }
    result
  }

}
