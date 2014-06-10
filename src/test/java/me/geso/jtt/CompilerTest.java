package me.geso.jtt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import me.geso.jtt.parser.ParserError;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.JSlateException;
import me.geso.jtt.vm.VM;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CompilerTest {
	Compiler compiler = new Compiler();
	TemplateLoader loader = new TemplateLoader(null, null);
	VM vm = new VM(compiler, loader, null, null);

	@Test
	public void test() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge", eval("hoge"));
	}

	@Test
	public void testInt() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5963", eval("hoge[% 5963 %]"));
	}

	@Test
	public void testAdd() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5963", eval("hoge[% 5900 + 63 %]"));
	}

	@Test
	public void testSub() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5837", eval("hoge[% 5900 - 63 %]"));
	}

	@Test
	public void testElem() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("a", 5963);

		assertEquals("5963", eval("[% a %]", vars));
	}

	@Test
	public void testElem2() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("a", 5963);

		assertEquals("5966", eval("[% a + 3 %]", vars));
	}

	@Test
	public void testMultiply() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("12", eval("[% 4 * 3 %]"));
	}

	@Test
	public void testDivide() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("4", eval("[% 12 / 3 %]"));
	}

	@Test
	public void testParen() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("20", eval("[% (3+2)*4 %]"));
	}

	@Test
	public void testDouble() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("3.14", eval("[% 3.14 %]"));
	}

	@Test
	public void testDoubleMultiply() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("6.22", eval("[% 3.11 * 2 %]"));
	}

	@Test
	public void testEscape() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("x", "<>");
		vars.put("y", new JTTRawString("<>"));

		assertEquals("<>&lt;&gt;<>", eval("<>[% x %][% y %]", vars));
	}

	@Test
	public void testForeach() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("y", Lists.newArrayList(5, 9, 6, 3));

		assertEquals("5963", eval("[% FOR x IN y %][% x %][% END %]", vars));
	}

	@Test
	public void testEqauls() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("falsetrue", eval("[% 5==3 %][% 3 == 3 %]"));

		assertEquals("false", eval("[% 3 == 2 %]", ImmutableMap.of()));
		assertEquals("true", eval("[% 3 == 3 %]", ImmutableMap.of()));
		assertEquals("false", eval("[% 'hoge' == 'fuga' %]", ImmutableMap.of()));
		assertEquals("true", eval("[% 'hoge' == 'hoge' %]", ImmutableMap.of()));
		assertEquals("false", eval("[% null == 'hoge' %]", ImmutableMap.of()));
		assertEquals("false", eval("[% 'hoge' == null %]", ImmutableMap.of()));
		assertEquals("true", eval("[% null == null %]", ImmutableMap.of()));
	}

	@Test
	public void testGt() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true false false", eval("[% 5>3 %] [% 3>3 %] [% 3>5 %]"));
	}

	@Test
	public void testGe() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% 5>=3 %]"));
	}

	@Test
	public void testGe2() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true true false",
				eval("[% 5>=3 %] [% 3>=3 %] [% 3>=5 %]"));
	}

	@Test
	public void testLT() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false false true", eval("[% 5<3 %] [% 3<3 %] [% 3<5 %]"));
	}

	@Test
	public void testLE() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false true true",
				eval("[% 5<=3 %] [% 3<=3 %] [% 3<=5 %]"));
	}

	@Test
	public void testArray() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("123", eval("[% FOR x IN [1,2,3,] %][% x %][% END %]"));
	}

	@Test
	public void testMod() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("2", eval("[% 62 % 3 %]"));
	}

	@Test
	public void testTrue() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true %]"));
	}

	@Test
	public void testFalse() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false", eval("[% false %]"));
	}

	@Test
	public void testIfTrue() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hogefuga", eval("[% IF true %]hoge[% END %]fuga"));
	}

	@Test
	public void testIfFalse() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("fuga", eval("[% IF false %]hoge[% END %]fuga"));
	}

	@Test
	public void testIfElsIfFalse() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("fuga",
				eval("[% IF false %]hoge[% ELSIF false %]piyo[% END %]fuga"));
	}

	@Test
	public void testIfElsIf() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("piyofuga",
				eval("[% IF false %]hoge[% ELSIF true %]piyo[% END %]fuga"));
	}

	@Test
	public void testIfElse() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("ooofuga",
				eval("[% IF false %]hoge[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testIfElsIfElse() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"ooofuga",
				eval("[% IF false %]hoge[% ELSIF false %]piyo[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testIfElsIfTrueElse() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"piyofuga",
				eval("[% IF false %]hoge[% ELSIF true %]piyo[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testString() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge", eval("[% \"hoge\" %]"));
	}

	@Test
	public void testStringConcat() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("hoge", eval("[% \"ho\" _ \"ge\" %]"));
	}

	@Test
	public void testSet() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("3", eval("[% SET s=3 %][% s %]"));
	}

	@Test
	public void testWhile() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals(
				"321ok",
				eval("[% SET x=3 %][% WHILE x > 0 %][% x %][% SET x = x - 1 %][% END %]ok"));
	}

	@Test
	public void testWhileFalse() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("ok", eval("[% WHILE false %]fail[% END %]ok"));
	}

	@Test
	public void testForLast() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals(
				"abok",
				eval("[% FOR x IN o %][% IF x==\"c\" %][% LAST %][% END %][% x %][% END %]ok",
						vars));
	}

	@Test
	public void testWhileLast() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals("54ok", eval("[% SET x=5 %][% WHILE x>0 %][% IF x==3 %][% LAST %][% END %][% x %][% SET x=x-1 %][% END %]ok", vars));
	}

	@Test
	public void testAssign() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("ok", eval("[% x=5 %]ok"));
	}

	@Test
	public void testForNext() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals("124ok", eval("[% x=5 %][% FOR x IN [1,2,3,4] %][% IF x==3 %][% NEXT %][% END %][% x %][% x=x-1 %][% END %]ok", vars));
	}

	@Test
	public void testConditionalOperator() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("43ok", eval("[% true ? 4 : 9 %][% false ? 5 : 3 %]ok"));
	}

	@Test
	public void testMapAccess() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", ImmutableMap.of("hoge", "fuga"));

		assertEquals("fuga", eval("[% o.hoge %]", vars));
	}

	@Test
	public void testMapLiteral() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("fuga", eval("[% {hoge=>\"fuga\", gogo=>4649}.hoge %]", vars));
	}

	@Test
	public void testSwitch() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Irep irep = compiler
				.compile("-", "[% SWITCH n %][% CASE 1 %]one[% CASE 2 %]two[% CASE %]more[% END %]");
		// System.out.println(new Disassembler().disasm(irep));

		{
			Map<String, Object> vars = new HashMap<String, Object>();
			vars.put("n", 1);
			String got = vm.run(irep, vars);
			assertEquals("one", got);
		}

		{
			Map<String, Object> vars = new HashMap<String, Object>();
			vars.put("n", 2);
			String got = vm.run(irep, vars);
			assertEquals("two", got);
		}

		{
			Map<String, Object> vars = new HashMap<String, Object>();
			vars.put("n", 3);
			String got = vm.run(irep, vars);
			assertEquals("more", got);
		}

		{
			Map<String, Object> vars = new HashMap<String, Object>();
			String got = vm.run(irep, vars);
			assertEquals("more", got);
		}
	}

	@Test
	public void testLowerCase() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("hoge", eval("[% lc(\"HoGe\") %]"));
	}

	@Test
	public void testBuiltinFunctionUri() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("%26+%2B3", eval("[% uri(\"& +3\") %]", vars));
	}

	@Test
	public void testUpperCase() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("HOGE", eval("[% uc(\"HoGe\") %]"));
	}

	@Test
	public void testSprintf() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("hehe 004, ahaha", eval("[% sprintf(\"hehe %03d, %s\", 4, \"ahaha\") %]"));
	}

	@Test
	public void testRange() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("1,2,3,4,5,", eval("[% FOR x IN 1..5 %][% x %],[% END %]"));
	}

	@Test
	public void testNot() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false,true", eval("[% !true %],[% !false %]"));
	}

	@Test
	public void testPipe() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge%2B%26", eval("[% \"hoge+&\" | uri %]"));
	}

	@Test
	public void testMethodCall() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("4", eval("[% [5,9,6,3].size() %]"));
	}

	@Test
	public void testMethodCall2() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("ge", eval("[% 'hoge'.substring(2) %]"));
	}

	@Test
	public void testMethodCall3() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("urge", eval("[%  'hamburger'.substring(4, 8) %]"));
	}

	@Test
	public void testAndAnd() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true && true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% true && false %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false && true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false && false %]", ImmutableMap.of()));
	}

	@Test
	public void testLooseAnd() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("true", eval("[% true AND true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% true AND false %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false AND true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false AND false %]", ImmutableMap.of()));
	}

	@Test
	public void testOrOr() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true || true %]", ImmutableMap.of()));
		assertEquals("true", eval("[% true || false %]", ImmutableMap.of()));
		assertEquals("true", eval("[% false || true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false || false %]", ImmutableMap.of()));
	}

	@Test
	public void testLooseOr() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true OR true %]", ImmutableMap.of()));
		assertEquals("true", eval("[% true OR false %]", ImmutableMap.of()));
		assertEquals("true", eval("[% false OR true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false OR false %]", ImmutableMap.of()));
	}

	@Test
	public void testNE() throws JSlateException, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% 3 != 2 %]", ImmutableMap.of()));
		assertEquals("false", eval("[% 3 != 3 %]", ImmutableMap.of()));
		assertEquals("true", eval("[% 'hoge' != 'fuga' %]", ImmutableMap.of()));
		assertEquals("false", eval("[% 'hoge' != 'hoge' %]", ImmutableMap.of()));
		assertEquals("true", eval("[% null != 'hoge' %]", ImmutableMap.of()));
		assertEquals("true", eval("[% 'hoge' != null %]", ImmutableMap.of()));
		assertEquals("false", eval("[% null != null %]", ImmutableMap.of()));
	}

	@Test
	public void testArrayAccess() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"9",
				eval("[% ary[$i] %]",
						ImmutableMap.of("i", 1, "ary",
								Lists.newArrayList(5, 9, 6, 3))));
	}

	@Test
	public void testMapIndex() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"fuga",
				eval("[% map[$k] %]",
						ImmutableMap.of("k", "hoge", "map",
								ImmutableMap.of("hoge", "fuga"))));
		assertEquals(
				"fuga",
				eval("[% map[k] %]",
						ImmutableMap.of("k", "hoge", "map",
								ImmutableMap.of("hoge", "fuga"))));
	}

	@Test
	public void testArrayDollarVarAccess() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"9",
				eval("[% ary.$i %]",
						ImmutableMap.of("i", 1, "ary",
								Lists.newArrayList(5, 9, 6, 3))));
	}

	@Test
	public void testMapDollarVarAccess() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"fuga",
				eval("[% map.$key %]", ImmutableMap.of("key", "hoge", "map",
						ImmutableMap.of("hoge", "fuga", "hige", "hage"))));
	}

	@Test
	public void testDollarVarAccess() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("4", eval("[% $i %]", ImmutableMap.of("i", 4)));
	}

	@Test
	public void testLoopCount() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("1234",
				eval("[% FOR x IN [5,9,6,3] %][% loop.getCount() %][% END %]"));
	}

	@Test
	public void testLoopHasNext() throws JSlateException, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("truefalse",
				eval("[% FOR x IN [5,9] %][% loop.hasNext() %][% END %]"));
	}

	private String eval(String src) throws ParserError, JSlateException,
			IOException, TemplateLoadingError {
		return eval(src, new HashMap<String, Object>());
	}

	private String eval(String src, Map<String, Object> vars)
			throws ParserError, JSlateException, IOException,
			TemplateLoadingError {
		Irep irep = compiler.compile("-", src);
		return vm.run(irep, vars);
	}
}
