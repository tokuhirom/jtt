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
  right	= last next

## INCOMPATIBILITY WITH Templte-Toolkit

 * We need casting operator.
 * 3 < 3.14 does not works correctly.
  * Because these values are *not* comparable.
    * `java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.Double`
 * `[% FOR x IN [1..10] %]` does not works.
  * Because you don't need to wrap by brackets.
  * I can't understand why TT2 needs brackets.
  * In JTT, you need to write this code as `[% FOR x IN 1..10 %]`.
 * [% a AND b %] style `AND`, `OR` operator was not supported.
   * You can use `&&` and `||` operators instead.

## FAQ

### Why  JTT doesn't support MACRO?

You can do it with INCLUDE or JavaScript.
