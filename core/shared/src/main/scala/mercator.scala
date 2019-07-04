/*
  
  Mercator, version 0.1.1. Copyright 2018 Jon Pretty, Propensive Ltd.

  The primary distribution site is: https://propensive.com/

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
  this file except in compliance with the License. You may obtain a copy of the
  License at
  
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.

*/
package mercator

import scala.language.higherKinds
import scala.reflect.macros._
import scala.collection.generic.CanBuildFrom

import language.experimental.macros

object `package` {
  implicit def monadic[F[_]]: Monadic[F] =
    macro Mercator.instantiate[F[Nothing]]
  
  final implicit class Ops[M[_], A](val value: M[A]) extends AnyVal {
    @inline def flatMap[B](fn: A => M[B])(implicit monadic: Monadic[M]): M[B] =
      monadic.flatMap[A, B](value)(fn)

    @inline def map[B](fn: A => B)(implicit monadic: Monadic[M]): M[B] =
      monadic.map[A, B](value)(fn)
    
    @inline def filter(fn: A => Boolean)(implicit monadic: MonadicFilter[M]): M[A] =
      monadic.filter[A](value)(fn)
  }

  final implicit class CollOps[M[_], Coll[T] <: Iterable[T], A](val value: Coll[M[A]]) extends AnyVal {
    @inline def sequence(implicit monadic: Monadic[M], cbf: CanBuildFrom[Nothing, A, Coll[A]]): M[Coll[A]] =
      value.foldLeft(monadic.point(List[A]()): M[List[A]]) { (acc, next) =>
        acc.flatMap { xs => next.map(_ :: xs) }
      }.map(_.reverse.to[Coll])
  }
  
  final implicit class TraversableOps[Coll[T] <: Iterable[T], A](val value: Coll[A]) extends AnyVal {
    @inline def traverse[B, M[_]](fn: A => M[B])(implicit monadic: Monadic[M], cbf: CanBuildFrom[Nothing, B, Coll[B]]): M[Coll[B]] =
      value.foldLeft(monadic.point(List[B]())) { (acc, next) =>
        acc.flatMap { xs => fn(next).map(_ :: xs) }
      }.map(_.reverse.to[Coll])
  }
}

object Mercator {
  def instantiate[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val typeConstructor = weakTypeOf[F].typeConstructor
    val mockType = appliedType(typeConstructor, typeOf[Mercator.type])
    val companion = typeConstructor.dealias.typeSymbol.companion
    val returnType = scala.util.Try(c.typecheck(q"$companion.apply(_root_.mercator.Mercator)").tpe)
    val pointApplication = if(returnType.map(_ <:< mockType).getOrElse(false)) q"${companion.asModule}(value)" else {
      val subtypes = typeConstructor.typeSymbol.asClass.knownDirectSubclasses.filter { sub =>
        c.typecheck(q"${sub.companion}.apply(_root_.mercator.Mercator)").tpe <:< mockType
      }.map { sub => q"${sub.companion}.apply(value)" }
      if(subtypes.size == 1) subtypes.head
      else c.abort(c.enclosingPosition, s"mercator: unable to derive Monadic instance for type constructor $typeConstructor")
    }

    val filterMethods: List[Tree] = if(mockType.typeSymbol.info.member(TermName("filter")) == NoSymbol) Nil
      else List(q"def filter[A](value: Monad[A])(fn: A => Boolean) = value.filter(fn)")

    val instantiation =
      if(filterMethods.isEmpty) tq"_root_.mercator.Monadic[$typeConstructor]"
      else tq"_root_.mercator.MonadicFilter[$typeConstructor]"

    q"""
      new $instantiation {
        def point[A](value: A): Monad[A] = $pointApplication
        def flatMap[A, B](from: Monad[A])(fn: A => Monad[B]): Monad[B] = from.flatMap(fn)
        def map[A, B](from: Monad[A])(fn: A => B): Monad[B] = from.map(fn)
        ..$filterMethods
      }
    """
  }
}

trait Monadic[F[_]] {
  type Monad[T] = F[T]
  def point[A](value: A): F[A]
  def flatMap[A, B](from: F[A])(fn: A => F[B]): F[B]
  def map[A, B](from: F[A])(fn: A => B): F[B]
}

trait MonadicFilter[F[_]] extends Monadic[F] {
  def filter[A](value: F[A])(fn: A => Boolean): F[A]
}
