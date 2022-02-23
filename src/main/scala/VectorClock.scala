trait VectorClock {
  def tick(str: String): Unit

  def merge(str: String): Unit

  def compare(str: String): Unit
}
