package eie.io
import java.security.MessageDigest

object MD5 {

  def apply(str: String): String = {
    val digest = MessageDigest.getInstance("MD5")
    Hex(digest.digest(str.getBytes))
  }

}
