object Logger {
  def println(s: Throwable): Unit = {
    s.printStackTrace(System.out)
  }
  def println(s: Any): Unit = {
    System.out.println(s)
  }
}
