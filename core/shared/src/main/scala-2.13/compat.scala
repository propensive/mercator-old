package mercator

import scala.collection.Factory
import language.higherKinds

final class CollOps[M[_], Coll[T] <: IterableOnce[T], A](val value: Coll[M[A]]) extends AnyVal {
  @inline def sequence(implicit monadic: Monadic[M], factory: Factory[A, Coll[A]]): M[Coll[A]] =
    value.iterator.foldLeft(monadic.point(List[A]()): M[List[A]]) { (acc, next) =>
      acc.flatMap { xs => next.map(_ :: xs) }
    }.map { xs => factory.fromSpecific(xs.reverseIterator) }
}

final class TraversableOps[Coll[T] <: IterableOnce[T], A](val value: Coll[A]) extends AnyVal {
  @inline def traverse[B, M[_]](fn: A => M[B])(implicit monadic: Monadic[M], factory: Factory[B, Coll[B]]): M[Coll[B]] =
    value.iterator.foldLeft(monadic.point(List[B]())) { (acc, next) =>
      acc.flatMap { xs => fn(next).map(_ :: xs) }
    }.map { xs => factory.fromSpecific(xs.reverseIterator) }
}

