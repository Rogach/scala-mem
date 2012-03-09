Scala memorizers
================

This is a simple project, that provides simple memorizer and parallelizer for anonymous recursive functions, using Y combinator. (for parallelizer, it uses Akka)

For more details, take a look at http://rogach-scala.blogspot.com/2012/03/memorizers-in-scala.html

Example:

    val recursiveFibonacci = memr[Int,Int](fib => n => if (n < 2) 1 else fib(n - 1) + fib(n - 2))
    recusiveFibonacci(5) // 8