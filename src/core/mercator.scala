/*

    Mercator, version 0.4.0. Copyright 2018-20 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
package mercator

import scala.language.higherKinds
import scala.reflect.macros._
import scala.collection.generic.CanBuildFrom

import language.experimental.macros

import language.reflectiveCalls

object `package` {
  implicit def monadic[F[_]]: Monadic[F] =
    macro Mercator.instantiate.monadic[F[Nothing]]
  
  final implicit class Ops[M[_], A](val value: M[A]) extends AnyVal {
    @inline def flatMap[B](fn: A => M[B])(implicit monadic: Monadic[M]): M[B] =
      monadic.flatMap[A, B](value)(fn)

    @inline def map[B](fn: A => B)(implicit functor: Functor[M]): M[B] =
      functor.map[A, B](value)(fn)
    
    @inline def filter(fn: A => Boolean)(implicit filterable: Filterable[M]): M[A] =
      filterable.filter[A](value)(fn)
  }
  
  implicit def functor[F[_]]: Functor[F] =
    macro Mercator.instantiate.functor[F[Nothing]]
  
  implicit def filterable[F[_]]: Filterable[F] =
    macro Mercator.instantiate.filterable[F[Nothing]]

  implicit def collOps[M[_], Coll[T] <: Traversable[T], A](value: Coll[M[A]]): CollOps[M, Coll, A] =
    new CollOps[M, Coll, A](value)

  implicit def traversableOps[Coll[T] <: Traversable[T], A](value: Coll[A]): TraversableOps[Coll, A] =
    new TraversableOps[Coll, A](value)
}

object Mercator {

  object instantiate {
    def functor[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = common(c).functor
    def monadic[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = common(c).monadic
    def filterable[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = common(c).filterable
  }

  def common[F: c.WeakTypeTag](c: whitebox.Context) = new {
    import c.universe._

    private val typeConstructor = weakTypeOf[F].typeConstructor
    private val mockType = appliedType(typeConstructor, typeOf[Mercator.type])
    private val companion = typeConstructor.dealias.typeSymbol.companion
    private val returnType = scala.util.Try(c.typecheck(q"$companion.apply(_root_.mercator.Mercator)").tpe)
    
    private def pointAp =
      if(returnType.map(_ <:< mockType).getOrElse(false)) q"${companion.asModule}(value)" else {
        val subtypes = typeConstructor.typeSymbol.asClass.knownDirectSubclasses.filter { sub =>
          c.typecheck(q"${sub.companion}.apply(_root_.mercator.Mercator)").tpe <:< mockType
        }.map { sub => q"${sub.companion}.apply(value)" }
        if(subtypes.size == 1) subtypes.head
        else c.abort(c.enclosingPosition, s"mercator: unable to derive Monadic instance for type constructor $typeConstructor")
      }

    def monadic: c.Tree = q"""
      new _root_.mercator.Monadic[$typeConstructor] {
        def point[A](value: A): Apply[A] = ${pointAp}
        def map[A, B](from: Apply[A])(fn: A => B): Apply[B] = from.map(fn)
        def flatMap[A, B](from: Apply[A])(fn: A => Apply[B]): Apply[B] = from.flatMap(fn)
      }
    """
  
    def functor: c.Tree = q"""
      new _root_.mercator.Functor[$typeConstructor] {
        def point[A](value: A): Apply[A] = ${pointAp}
        def map[A, B](from: Apply[A])(fn: A => B): Apply[B] = from.map(fn)
      }
    """

    def filterable: c.Tree = q"""
      new _root_.mercator.Filterable[$typeConstructor] {
        def filter[A](value: Apply[A])(fn: A => Boolean): Apply[A] = value.filter(fn)
      }
    """
  }
}

trait Functor[F[_]] {
  type Apply[X] = F[X]
  def point[A](value: A): F[A]
  def map[A, B](from: F[A])(fn: A => B): F[B]
}

trait Monadic[F[_]] extends Functor[F] {
  def flatMap[A, B](from: F[A])(fn: A => F[B]): F[B]
}

trait Filterable[F[_]] {
  type Apply[X] = F[X]
  def filter[A](value: F[A])(fn: A => Boolean): F[A]
}
