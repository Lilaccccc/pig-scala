package pig
package utils.base

import kyo.Ansi.*

object ColoredLog {
  extension (message: Any) {
    def e: String = s"$message".red
    def w: String  = s"$message".yellow
    def i: String  = s"$message".green
    def d: String = s"$message".blue
  }
}
