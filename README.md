# JTT

Fast template engine for Java.
This is a port of Perl5's popular template engine named Template-Toolkit 2.


## Project status

In development.

## Architecture

  * VM has TemplateLoader

## ERD

We need ER Diagram.

## Operators

```
  left	terms and list operators (leftward)
  left	.
  right	!
  left	* / %
  left	+ - _
  nonassoc	named unary operators
  nonassoc	< > <= >=
  nonassoc	== !=
  left	&&
  left	||
  nonassoc	..
  right	?:
  right	=
  left   AND
  left   OR
```

## INCOMPATIBILITY WITH Templte-Toolkit

 * We need casting operator.
 * 3 < 3.14 does not works correctly.
  * Because these values are *not* comparable.
    * `java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.Double`
 * `[% FOR x IN [1..10] %]` does not works.
  * Because you don't need to wrap by brackets.
  * I can't understand why TT2 needs brackets.
  * In JTT, you need to write this code as `[% FOR x IN 1..10 %]`.
 * VMethod is not supported. Because Java objects have own built-in methods.

## Caching

You can cache the compilation result.

JTT's parser/compiler is slow. If you are using JTT in the web application, I recommend to use caching feature.

You can use JCS for caching(I will provide jtt-jcs package after few months...(maybe)).

## FAQ

### Why  JTT doesn't support MACRO?

You can do it with INCLUDE or JavaScript.

## Is there an "Casting operator"?

How do I parse casting operator?
