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
package mercator.tests

import mercator._
import probably._
import contextual.examples.scalac._
import contextual.examples.fqt._

import scala.language.higherKinds

object Tests extends Suite("Mercator tests") {
  
  def run(test: Runner): Unit = {
    
    test("derive functor for Option") {
      scalac"functor[Option]"
    }.assert(_ == Returns(fqt"mercator.Functor[Option]"))

    test("derive monadic for Either") {
      val x = scalac"monadic[({ type L[W] = Either[String, W] })#L]"
      println(x)
      x
    }.assert(_ == Returns(Fqt("mercator.Monadic[[W]scala.util.Either[String,W]]")))

    test("derive filterable for Seq") {
      scalac"filterable[Seq]"
    }.assert(_ == Returns(fqt"mercator.Filterable[Seq]"))

    def increment[F[_]: Monadic](xs: F[Int]) = for(x <- xs) yield x + 1

    test("increment list elements") {
      increment(List(1, 2, 3))
    }.assert(_ == List(2, 3, 4))

    test("increment over option") {
      increment(Option(4))
    }.assert(_ == Option(5))

    test("increment over iterable") {
      increment(Iterable(4))
    }.assert(_ == Iterable(5))

    test("increment over either (left branch)") {
      increment[({ type L[W] = Either[String, W] })#L](Left(""))
    }.assert(_ == Left(""))

    test("increment over either (right branch)") {
      increment[({ type L[W] = Either[String, W] })#L](Right(6))
    }.assert(_ == Right(7))
  }
}
