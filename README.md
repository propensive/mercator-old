[![Latest version](https://index.scala-lang.org/propensive/mercator/latest.svg)](https://index.scala-lang.org/propensive/mercator)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.propensive/mercator_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.propensive/mercator_2.12)

# Mercator

Mercator is a macro for automatically constructing evidence that a known type
may be used in a for-comprehension, for abstracting over monad-like types with
no impact on performance.

This allows us to write code against generic type constructors which can assume
that they are usable in a for-comprehension, without having the evidence that
they are until the application point when the type constructor is known, at
which point, Mercator will construct it.

## Usage

It is not possible to write code such as,
```
// does not compile
def increment[F[_]](xs: F[Int]) = for(x <- xs) yield x + 1
```
because the compiler is not able to enforce the constraint that the type
constructor `F[_]` provides the methods `map` and `flatMap` (with the correct
signatures) which are necessary for the for-comprehension to compile.

With Mercator, it is possible to demand an implicit instance of `Monadic[F]` to
enforce this constraint. Mercator will automatically instantiate such an
instance at the use-site for any type which has the required methods, like so,
```
import mercator._
def increment[F[_]: Monadic](xs: F[Int]) = for(x <- xs) yield x + 1
```

The methods `flatMap` and `map` will be provided to the instance of `F[_]` as
extension methods, using an implicit value class in the `mercator` package.
This incurs no allocations at runtime, and the performance overhead should be
zero or negligible.

## Point

An instance of `Monadic[F]` will generate an implementation of `point`, which
constructs a new instance of the type from a single value. This implementation
assumes the existence of an `apply` method on the type's companion object, and
that applying the value to it will produce a result of the correct type.

If this is not the case, Mercator will try to find a unique subtype of `F[_]`
whose companion object has an apply method taking a single value and returning
the correct type. In the case of `Either` or Scalaz's `\/`, this will do the
right thing.


