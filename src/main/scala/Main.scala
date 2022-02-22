@main def hello: Unit = 
  println("Hello world!")
  println(msg)

  var vc = VectorClock(1, 3) 
  vc.localTick()
  vc.mergeWith(vc)
  vc.compareWith(vc)

def msg = "I was compiled by Scala 3. :)"
