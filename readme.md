[<img alt="GitHub Workflow" src="https://img.shields.io/github/workflow/status/propensive/mercator/Build/main?style=for-the-badge" height="24">](https://github.com/propensive/mercator/actions)
[<img src="https://img.shields.io/badge/gitter-discuss-f00762?style=for-the-badge" height="24">](https://gitter.im/propensive/mercator)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/CHCPjERybv)
[<img src="https://img.shields.io/matrix/propensive.mercator:matrix.org?label=MATRIX&color=0dbd8b&style=for-the-badge" height="24">](https://app.element.io/#/room/#propensive.mercator:matrix.org)
[<img src="https://img.shields.io/twitter/follow/propensive?color=%2300acee&label=TWITTER&style=for-the-badge" height="24">](https://twitter.com/propensive)
[<img src="https://img.shields.io/maven-central/v/com.propensive/mercator-core_2.12?color=2465cd&style=for-the-badge" height="24">](https://search.maven.org/artifact/com.propensive/mercator-core_2.12)
[<img src="https://img.shields.io/badge/vent-propensive%2Fmercator-f05662?style=for-the-badge" height="24">](https://vent.dev)

<img src="/doc/images/github.png" valign="middle">

# Mercator

Mercator is a macro for automatically constructing evidence that a known type may be used in a for-comprehension, for abstracting over monad-like types with no impact on performance.  This allows us to write code against generic type constructors which can assume that they are usable in a for-comprehension, without having the evidence that they are until the application point when the type constructor is known, at which point, Mercator will construct it.

## Features

- abstracts over monad-like types
- constructs a monad typeclass instance for any type with `flatMap`, `map` and a "point" constructor
- makes intelligent guesses about identifying the "point" constructor for a type
- constructs a functor instance if only `map` and "point" are available


## Getting Started

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


## Status

Mercator is classified as __fledgling__. Propensive defines the following five stability levels for open-source projects:

- _embryonic_: for experimental or demonstrative purposes only, without guarantee of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement of designs
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

## Availability

Mercator&rsquo;s source is available on GitHub, and may be built with [Fury](https://github.com/propensive/fury) by
cloning the layer `propensive/mercator`.
```
fury layer clone -i propensive/mercator
```
or imported into an existing layer with,
```
fury layer import -i propensive/mercator
```
A binary is available on Maven Central as `com.propensive:mercator-core_<scala-version>:0.4.0`. This may be added
to an [sbt](https://www.scala-sbt.org/) build with:
```
libraryDependencies += "com.propensive" %% "mercator-core" % "0.4.0"
```

## Contributing

Contributors to Mercator are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/mercator/labels/good%20first%20issue"><img alt="label: good first issue"
src="https://img.shields.io/badge/-good%20first%20issue-67b6d0.svg" valign="middle"></a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Mercator easier.

Please __do not__ contact project maintainers privately with questions, as other users cannot then benefit from
the answers.

## Author

Mercator was designed and developed by [Jon Pretty](https://twitter.com/propensive), and commercial support and
training is available from [Propensive O&Uuml;](https://propensive.com/).



## License

Mercator is copyright &copy; 2018-20 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
