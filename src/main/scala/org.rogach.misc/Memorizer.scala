package org.rogach.misc

object Memorizer {

  // memorizer function
  // transparently wraps provided function into the function, that does not do unnececcary recalculations
  def mem[A,B](f:A=>B) = new Function[A,B] {
    private var cache = scala.collection.mutable.Map[A,B]()
    def apply(v:A):B = cache getOrElseUpdate(v,f(v))
  }
  
  // fixed-point combinator in scala
  def fix[A,B](f:(A=>B)=>(A=>B)):(A=>B) = f(fix(f))(_)

  // memorizer function, based on fixed-point combinator
  def memr[A,B](f:(A=>B)=>(A=>B)):(A=>B) = new Function[A,B] {
    private var cache = scala.collection.mutable.Map[A,B]()
    private def result(v:A):B = cache getOrElseUpdate(v, fix(f)(v))
    private def fix(f:(A=>B)=>(A=>B)):(A=>B) = f(result)(_)
    def apply(v:A):B = result(v)
  }
  
  import akka.dispatch.{Future, ExecutionContext, Await};
  // memorizer & parallelizer function
  def parmemr[A,B](f:((A=>Future[B]),ExecutionContext)=>(A=>Future[B])):(A=>Future[B]) = new Function[A,Future[B]] {
    import akka.dispatch.ExecutionContext;
    import java.util.concurrent.Executors;
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))    
    private var cache = scala.collection.mutable.Map[A,Future[B]]()
    private def result(v:A):Future[B] = cache getOrElseUpdate(v, fix(f)(v))
    private def fix(f:((A=>Future[B]),ExecutionContext)=>(A=>Future[B])):(A=>Future[B]) = f(result,ec)(_)
    def apply(v:A):Future[B] = result(v)
    def vle(v:A):B = Await.result(this(v), akka.util.Duration.Inf)
  }

  // some example usage, just in case
  def main(args:Array[String]) {
    // simple function memorization
    val fun = (a:Int) => { println("Call!"); a + 1}
    val funMem = mem(fun)
    funMem(1) // "Call!"
    funMem(1) // nothing printed
    
    val add = mem(((a:Int,b:Int) => { println("Add!"); a + b }).tupled)
    add(2,3)
    add(2,3)
    
    // recursive funtion memorization
    val rfun = memr[Int,Int](f => a => { println("rCall! " + a); if (a < 2) 1 else f(a - 1) + f(a - 2) })
    println((0 to 5).map(rfun).mkString(" "))
    
    // parallel function example
    // the following is a classic fibonacci function, just with a lot of makeup
    import akka.dispatch.Future;
    import akka.dispatch.Await;
    import akka.util.duration._;
    val parfib = parmemr[Int,BigInt] { (f,ec) => a =>
      implicit val e = ec
      if (a < 2) Future(BigInt(1))
      else {
        val f1 = f(a - 1)
        val f2 = f(a - 2)
        for {
          v1 <- f1
          v2 <- f2
        } yield { v1 + v2}
      }
    }
    println(Await.result(parfib(50), 1 minute))
  }
}
