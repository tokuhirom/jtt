# To-Do

This file lists To-Does for JTT project.

## DOING

* Make it register machine

## TODO

* run `[% INCLUDE "hoge" WITH foo=bar %]`
* compile `[% INCLUDE "hoge" WITH foo=bar %]`
* benchmarking
* register to sonatype
* Remove unused enum elements
* Constant folding
* We should deprecate SET_LVAR with register.
* `[% {}.getKeys() %]` ... Empty hash

## PENDING

* Should we add special op for loop.getCount()?

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
 * .. range constructor
 * lex unary ! operator
* parse unary ! operator
* run unary ! operator
* lex `[% hoge | uri %]`
* parse `[% hoge | uri %]`
* run `[% hoge | uri %]`
* `[% [1,2,3].size() %]`
* lex && operator
* parse && operator
* run && operator
* lex '!=' operator
* parse '!=' operator
* run '!=' operator
* || operator
* loose AND
* `[% foo != bar %]`
* loose OR
* `[% list.$key %]`
* `[% map.$key %]`
* `[% a[i] %]`
* Support user's function
* warnings listener
* `[% loop.getCount() %]`
* `[% __FILE__ %]`
* `[% __LINE__ %]`
* Deprecate TTLexBuilder.
* caching
* Make pool values unique.
  * Do not add same object twice in the constant pool.
  * It should be shared.
* parse `[% INCLUDE "hoge" WITH foo=bar %]`
* lex `[% WRAPPER %]`
* parse `[% WRAPPER %]`
* run `[% WRAPPER %]`
* Fix the test case for in memory template cache.
* Benchmarking with Velocity.
* Add LOAD_INT op.
* local variable should use list instead of map.
* Remove loop.getPC
* `[% FOR i IN [] %]` ... Empty array can't work in the iterator.
* Remove `addLazy(op, a, node)`. add() method should return code.
* Optimize `[% loop.count %]`
