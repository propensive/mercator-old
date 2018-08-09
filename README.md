[![Latest version](https://index.scala-lang.org/propensive/mercator/latest.svg)](https://index.scala-lang.org/propensive/mercator)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.propensive/mercator_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.propensive/mercator_2.12)

# Mercator

Mercator is a macro for automatically constructing evidence that a known type
may be used in a for-comprehension.

This allows us to write code against generic type constructors which can assume
that they are usable in a for-comprehension, without having the evidence that
they are until the application point when the type constructor is known, at
which point, Mercator will construct it.

