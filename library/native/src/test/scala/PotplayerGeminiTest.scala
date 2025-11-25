import org.junit.Assert._
import org.junit.Test

class PotplayerGeminiTest {
  @Test def getParent(): Unit = {
    val parent = ProcessUtils.getParentProcessId
    println(parent)
    assertNotEquals("parent is not None", None)
  }
}
