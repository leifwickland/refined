# refined
[![Build Status](https://img.shields.io/travis/fthomas/refined.svg)](https://travis-ci.org/fthomas/refined)
[![Download](https://img.shields.io/maven-central/v/eu.timepit/refined_2.11.svg)][search.maven]
[![Gitter](https://img.shields.io/badge/GITTER-join%20chat-brightgreen.svg)](https://gitter.im/fthomas/refined?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![codecov.io](https://img.shields.io/codecov/c/github/fthomas/refined.svg)](http://codecov.io/github/fthomas/refined)
[![Codacy Badge](https://img.shields.io/codacy/e4f25ef2656e463e8fed3f4f9314abdb.svg)](https://www.codacy.com/app/fthomas/refined)

**refined** is a Scala library for refining types with type-level predicates
which constrain the set of values described by the refined type. It started
as a port of the [refined][refined.hs] Haskell library (which also provides
an excellent motivation why this kind of library is useful).

A quick example:

```scala
import eu.timepit.refined._
import eu.timepit.refined.numeric._

// refineLit decorates the type of its parameter if it satisfies the
// given type-level predicate:
scala> refineLit[Positive](5)
res0: Int @@ Positive = 5

// The type Int @@ Positive is the type of all Int values that are
// greater than zero.

// If the parameter does not satisfy the predicate, we get a meaningful
// compile error:
scala> refineLit[Positive](-5)
<console>:34: error: Predicate failed: (-5 > 0).
              refineLit[Positive](-5)
                                 ^

// refineLit is a macro and only works with literals. To validate
// arbitrary (runtime) values we can use the refine function:
scala> refine[Positive](5)
res1: Either[String, Int @@ Positive] = Right(5)

scala> refine[Positive](-5)
res2: Either[String, Int @@ Positive] = Left(Predicate failed: (-5 > 0).)
```

Note that `@@` is [shapeless'][shapeless] type for tagging types.

**refined** also contains inference rules for converting between different
refined types. For example, `Int @@ Greater[_10]` can be safely converted
to `Int @@ Positive` because all integers greater than ten are also positive.
The type conversion of refined types is a compile-time operation that is
provided by the library:

```scala
import eu.timepit.refined.implicits._
import shapeless.nat._
import shapeless.tag.@@

scala> val a: Int @@ Greater[_5] = refineLit(10)
a: Int @@ Greater[_5] = 10

// Since every value greater than 5 is also greater than 4, a can be ascribed
// the type Int @@ Greater[_4]:
scala> val b: Int @@ Greater[_4] = a
b: Int @@ Greater[_4] = 10

// An unsound ascription leads to a compile error:
scala> val c: Int @@ Greater[_6] = a
<console>:34: error: invalid inference: Greater[_5] ==> Greater[_6]
       val b: Int @@ Greater[_6] = a
                                   ^
```

This mechanism allows to pass values of more specific types (e.g. `Int @@ Greater[_10]`)
to functions that take a more general type (e.g. `Int @@ Positive`) without manual
intervention.

## More examples

```scala
import shapeless.{ ::, HNil }
import eu.timepit.refined.boolean._
import eu.timepit.refined.char._
import eu.timepit.refined.collection._
import eu.timepit.refined.generic._
import eu.timepit.refined.string._

scala> refineLit[NonEmpty]("Hello")
res2: String @@ NonEmpty = Hello

scala> refineLit[NonEmpty]("")
<console>:27: error: Predicate isEmpty() did not fail.
            refineLit[NonEmpty]("")
                               ^

scala> type ZeroToOne = Not[Less[_0]] And Not[Greater[_1]]
defined type alias ZeroToOne

scala> refineLit[ZeroToOne](1.8)
<console>:27: error: Right predicate of (!(1.8 < 0) && !(1.8 > 1)) failed: Predicate (1.8 > 1) did not fail.
              refineLit[ZeroToOne](1.8)
                                  ^

scala> refineLit[AnyOf[Digit :: Letter :: Whitespace :: HNil]]('F')
res3: Char @@ AnyOf[Digit :: Letter :: Whitespace :: HNil] = F

scala> refineLit[MatchesRegex[W.`"[0-9]+"`.T]]("123.")
<console>:34: error: Predicate failed: "123.".matches("[0-9]+").
              refineLit[MatchesRegex[W.`"[0-9]+"`.T]]("123.")
                                                     ^

// The implicits object contains an implicit version of refineLit which is
// used here to validate that '3' is '3':
scala> val d1: Char @@ Equal[W.`'3'`.T] = '3'
d1: Char @@ Equal[Char('3')] = 3

scala> val d2: Char @@ Digit = d1
d2: Char @@ Digit = 3

scala> val d3: Char @@ Letter = d1
<console>:34: error: invalid inference: Equal[Char('3')] ==> Letter
       val d3: Char @@ Letter = d1
                                ^

scala> val r1: String @@ Regex = "(a|b)"
r1: String @@ Regex = (a|b)

scala> val r2: String @@ Regex = "(a|b"
<console>:37: error: Predicate isValidRegex("(a|b") failed: Unclosed group near index 4
(a|b
    ^
       val r2: String @@ Regex = "(a|b"
                                 ^
```

Note that `W` is a shortcut for [`shapeless.Witness`][singleton-types] which
provides syntax for singleton types.

## Installation

The latest version of the library is 0.0.3, which is built against Scala 2.11.

If you're using SBT, add the following to your build file:

    libraryDependencies += "eu.timepit" %% "refined" % "0.0.3"

Instructions for Maven and other build tools is available at [search.maven.org][search.maven].

**Note:** You currently need to add this resolver to your build due to a known
problem in the [tut](https://github.com/tpolecat/tut) plugin:

    resolvers += "tpolecat" at "http://dl.bintray.com/tpolecat/maven"

## Documentation

API documentation of the latest release is available at:
http://fthomas.github.io/refined/latest/api/

There are also further (type-checked) examples in the [`docs`][docs]
directory including one for defining [custom predicates][custom-pred].

[docs]: https://github.com/fthomas/refined/tree/master/docs
[custom-pred]: https://github.com/fthomas/refined/tree/master/docs/point.md

## Internals

**refined** basically consists of two parts, one for [refining types with
type-level predicates](#predicates) and the other for [converting between
different refined types](#inference-rules).

### Predicates

The refinement machinery is built of:

* Type-level predicates for refining other types, like `UpperCase`, `Positive`, or
  `LessEqual[_2]`. There are also higher order predicates for combining proper
  predicates like `And[_, _]`, `Or[_, _]`, `Not[_]`, `Forall[_]`, or `Size[_]`.

* A `Predicate` type class for validating a value of an unrefined type
  (like `Double`) against a type-level predicate (like `Positive`).

* A function `refine` and a macro `refineLit` that take a predicate `P`
  and some value of type `T`, validate this value with a `Predicate[P, T]`
  and return the value with type `T @@ P` if validation was successful or
  an error otherwise. The return type of `refine` is `Either[String, T @@ P]`
  while the `refineLit` returns a `T @@ P` or compilation fails. Since
  `refineLit` is a macro it only works with literal values.

### Inference rules

The type-conversions are built of:

* An `InferenceRule` type class that is indexed by two type-level predicates
  which states whether the second predicate can be logically derived from the
  first. `InferenceRule[Greater[_5], Positive]` would be an instance of a
  valid inference rule while `InferenceRule[Greater[_5], Negative]` would be
  an invalid inference rule.

* An implicit conversion defined as macro that casts a value of type `T @@ A`
  to type `T @@ B` if a valid `InferenceRule[A, B]` is in scope.

## Provided predicates

The library comes with these predefined predicates:

[`boolean`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/boolean.scala)

* `True`: constant predicate that is always `true`
* `False`: constant predicate that is always `false`
* `Not[P]`: negation of the predicate `P`
* `And[A, B]`: conjunction of the predicates `A` and `B`
* `Or[A, B]`: disjunction of the predicates `A` and `B`
* `Xor[A, B]`: exclusive disjunction of the predicates `A` and `B`
* `AllOf[PS]`: conjunction of all predicates in `PS`
* `AnyOf[PS]`: disjunction of all predicates in `PS`
* `OneOf[PS]`: exclusive disjunction of all predicates in `PS`

[`char`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/char.scala)

* `Digit`: checks if a `Char` is a digit
* `Letter`: checks if a `Char` is a letter
* `LetterOrDigit`: checks if a `Char` is a letter or digit
* `LowerCase`: checks if a `Char` is a lower case character
* `UpperCase`: checks if a `Char` is an upper case character
* `Whitespace`: checks if a `Char` is white space

[`collection`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/collection.scala)

* `Contains[U]`: checks if a `TraversableOnce` contains a value equal to `U`
* `Count[PA, PC]`: counts the number of elements in a `TraversableOnce` which
  satisfy the predicate `PA` and passes the result to the predicate `PC`
* `Empty`: checks if a `TraversableOnce` is empty
* `NonEmpty`: checks if a `TraversableOnce` is not empty
* `Forall[P]`: checks if the predicate `P` holds for all elements of a
  `TraversableOnce`
* `Exists[P]`: checks if the predicate `P` holds for some elements of a
  `TraversableOnce`
* `Head[P]`: checks if the predicate `P` holds for the first element of
  a `Traversable`
* `Index[N, P]`: checks if the predicate `P` holds for the element at
  index `N` of a sequence
* `Last[P]`: checks if the predicate `P` holds for the last element of
  a `Traversable`
* `Size[P]`: checks if the size of a `TraversableOnce` satisfies the predicate `P`
* `MinSize[N]`: checks if the size of a `TraversableOnce` is greater than
  or equal to `N`
* `MaxSize[N]`: checks if the size of a `TraversableOnce` is less than
  or equal to `N`

[`generic`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/generic.scala)

* `Equal[U]`: checks if a value is equal to `U`

[`numeric`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/numeric.scala)

* `Less[N]`: checks if a numeric value is less than `N`
* `LessEqual[N]`: checks if a numeric value is less than or equal to `N`
* `Greater[N]`: checks if a numeric value is greater than `N`
* `GreaterEqual[N]`: checks if a numeric value is greater than or equal to `N`
* `Positive`: checks if a numeric value is greater than zero
* `NonPositive`: checks if a numeric value is zero or negative
* `Negative`: checks if a numeric value is less than zero
* `NonNegative`: checks if a numeric value is zero or positive
* `Interval[L, H]`: checks if a numeric value is in the interval [`L`, `H`]

[`string`](https://github.com/fthomas/refined/blob/master/src/main/scala/eu/timepit/refined/string.scala)

* `EndsWith[S]`: checks if a `String` ends with the suffix `S`
* `MatchesRegex[R]`: checks if a `String` matches the regular expression `R`
* `Regex`: checks if a `String` is a valid regular expression
* `StartsWith[S]`: checks if a `String` starts with the prefix `S`

## Related projects

This library is inspired by the [refined][refined.hs] library for Haskell.
It even stole its name! Another Scala library that provides type-level
validations is [bond][bond].

## License

**refined** is licensed under the MIT license, available at http://opensource.org/licenses/MIT
and also in the [LICENSE](https://github.com/fthomas/refined/blob/master/LICENSE) file.

[bond]: https://github.com/fwbrasil/bond
[refined.hs]: http://nikita-volkov.github.io/refined
[search.maven]: http://search.maven.org/#search|ga|1|eu.timepit.refined
[shapeless]: https://github.com/milessabin/shapeless
[singleton-types]: https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#singleton-typed-literals