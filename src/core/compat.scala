/*

    Mercator, version 0.6.0. Copyright 2018-20 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
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

