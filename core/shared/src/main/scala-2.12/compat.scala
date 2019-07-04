package mercator

import scala.collection.generic.CanBuildFrom
import language.higherKinds

final class CollOps[M[_], Coll[T] <: Traversable[T], A](val value: Coll[M[A]]) extends AnyVal {
  @inline def sequence(implicit monadic: Monadic[M], cbf: CanBuildFrom[Nothing, A, Coll[A]]): M[Coll[A]] =
    value.foldLeft(monadic.point(List[A]()): M[List[A]]) { (acc, next) =>
      acc.flatMap { xs => next.map(_ :: xs) }
    }.map { xs => xs.reverseIterator.to[Coll] }
}

final class TraversableOps[Coll[T] <: Traversable[T], A](val value: Coll[A]) extends AnyVal {
  @inline def traverse[B, M[_]](fn: A => M[B])(implicit monadic: Monadic[M], cbf: CanBuildFrom[Nothing, B, Coll[B]]): M[Coll[B]] =
    value.foldLeft(monadic.point(List[B]())) { (acc, next) =>
      acc.flatMap { xs => fn(next).map(_ :: xs) }
    }.map { xs => xs.reverseIterator.to[Coll] }
}

