import Logger.println

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.concurrent.duration.DurationInt

object PotplayerGemini extends App {
  Future {
    blocking {
      try {
        ProcessUtils.exitAfterParentProcess()
      } catch {
        case e => println(e)
      }
    }
  }
  Gemini.loop()
}

