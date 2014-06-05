# JTT

Fast template engine for Java.
This is a port of Perl5's popular template engine named Template-Toolkit 2.

## INCOMPATIBILITY WITH Templte-Toolkit

 * We need casting operator.
 * 3 < 3.14 does not works correctly.
  * Because these values are *not* comparable.
    * `java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.Double`

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
  left	|| //
  nonassoc	..  ...
  right	?:
  right	= last next
  right	not
  left	and
  left	or

## DOING


## TODO

 * `[% loop.count %]`
* `[% INCLUDE "hoge" WITH foo=bar %]`
 * `[% [1,2,3].size() %]`
 * warnings listener
 * and
 * or
 * ! operator
 * && operator
 * || operator
* `[% hoge | uri %]`
 * caching

## いらないっぽい?

* run WHILE `NEXT`

## DONE

 * `[% (3+2)*4 %]`
 * Automatic escape
 * `[% FOR x IN y %]`
 * ==
 * <
 * >
 * `<=`
 * `=>`
 * `[% (1+2)*3 ]`
 * `[% 4 % 2 %]`
 * array construction operator `[1,2,3]`
 * true literal
 * false literal
 * null literal
 * `[% IF ... %]`
* `"hogehoge"`
* `[% WHILE y %]`
* `[% SET x=y %]`
* `[% INCLUDE "hoge" %]`
* parser `{a=>b}`
* run `{a=>b}`
* parse `[% SWITCH -%]`
* `[%- foo -%]`
* compile `NEXT`
* run `[% SWITCH -%]`
* `[% sprintf("%.2f", 35.113) %]`
* `[% lc("HOGE") %]`
* Comments. [%# this entire directive is ignored no
    matter how many lines it wraps onto
%]
* WHILE `LAST`
* run FOR `NEXT`
* ? : operator
