package me.geso.jtt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.escape.HTMLEscaper;
import me.geso.jtt.exception.JTTCompilerError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.tt.TTSyntax;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.VM;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CompilerTest {
	Syntax syntax = new TTSyntax();
	TemplateLoader loader = new TemplateLoader(null, null);

	@Test
	public void test() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge", eval("hoge"));
	}

	// (template (raw_string hoge) (expression (integer 5963)))
	@Test
	public void testInt() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5963", eval("hoge[% 5963 %]"));
	}

	@Test
	public void testAdd() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5963", eval("hoge[% 5900 + 63 %]"));
	}

	@Test
	public void testSub() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge5837", eval("hoge[% 5900 - 63 %]"));
	}

	@Test
	public void testElem() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("a", 5963);

		assertEquals("5963", eval("[% a %]", vars));
	}

	@Test
	public void testElem2() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("a", 5963);

		assertEquals("5966", eval("[% a + 3 %]", vars));
	}

	@Test
	public void testMultiply() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("12", eval("[% 4 * 3 %]"));
	}

	@Test
	public void testDivide() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("4", eval("[% 12 / 3 %]"));
	}

	@Test
	public void testParen() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("20", eval("[% (3+2)*4 %]"));
	}

	@Test
	public void testDouble() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("3.14", eval("[% 3.14 %]"));
	}

	@Test
	public void testDoubleMultiply() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("6.22", eval("[% 3.11 * 2 %]"));
	}

	@Test
	public void testEscape() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("x", "<>");
		vars.put("y", new JTTRawString("<>"));

		assertEquals("<>&lt;&gt;<>", eval("<>[% x %][% y %]", vars));
	}

	@Test
	public void testForeach() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("y", Lists.newArrayList(5, 9, 6, 3));

		assertEquals("5963", eval("[% FOR x IN y %][% x %][% END %]", vars));
	}

	@Test
	public void testEquals() throws JTTCompilerError, ParserError, IOException,
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
	public void testGt() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true false false", eval("[% 5>3 %] [% 3>3 %] [% 3>5 %]"));
	}

	@Test
	public void testGe() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% 5>=3 %]"));
	}

	@Test
	public void testGe2() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true true false",
				eval("[% 5>=3 %] [% 3>=3 %] [% 3>=5 %]"));
	}

	@Test
	public void testLT() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false false true", eval("[% 5<3 %] [% 3<3 %] [% 3<5 %]"));
	}

	@Test
	public void testLE() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false true true",
				eval("[% 5<=3 %] [% 3<=3 %] [% 3<=5 %]"));
	}

	@Test
	public void testArray() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("5963", eval("[% FOR x IN [5,9,6,3,] %][% x %][% END %]"));
		// assertEquals("", eval("[% FOR x IN [] %][% x %][% END %]"));
	}

	@Test
	public void testMod() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("2", eval("[% 62 % 3 %]"));
	}

	@Test
	public void testTrue() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true %]"));
	}

	@Test
	public void testFalse() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false", eval("[% false %]"));
	}

	@Test
	public void testIfTrue() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hogefuga", eval("[% IF true %]hoge[% END %]fuga"));
	}

	@Test
	public void testIfFalse() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("fuga", eval("[% IF false %]hoge[% END %]fuga"));
	}

	@Test
	public void testIfElsIfFalse() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("fuga",
				eval("[% IF false %]hoge[% ELSIF false %]piyo[% END %]fuga"));
	}

	@Test
	public void testIfElsIf() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("piyofuga",
				eval("[% IF false %]hoge[% ELSIF true %]piyo[% END %]fuga"));
	}

	@Test
	public void testIfElse() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("ooofuga",
				eval("[% IF false %]hoge[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testIfElsIfElse() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"ooofuga",
				eval("[% IF false %]hoge[% ELSIF false %]piyo[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testIfElsIfTrueElse() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"piyofuga",
				eval("[% IF false %]hoge[% ELSIF true %]piyo[% ELSE %]ooo[% END %]fuga"));
	}

	@Test
	public void testString() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge", eval("[% \"hoge\" %]"));
	}

	@Test
	public void testStringConcat() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("hoge", eval("[% \"ho\" _ \"ge\" %]"));
	}

	@Test
	public void testSet() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("3", eval("[% SET s=3 %][% s %]"));
	}

	@Test
	public void testWhile() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		assertEquals(
				"321ok",
				eval("[% SET x=3 %][% WHILE x > 0 %][% x %][% SET x = x - 1 %][% END %]ok"));
	}

	@Test
	public void testWhileFalse() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("ok", eval("[% WHILE false %]fail[% END %]ok"));
	}

	@Test
	public void testForLast() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals(
				"abok",
				eval("[% FOR x IN o %][% IF x==\"c\" %][% LAST %][% END %][% x %][% END %]ok",
						vars));
	}

	@Test
	public void testWhileLast() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals(
				"54ok",
				eval("[% SET x=5 %][% WHILE x>0 %][% IF x==3 %][% LAST %][% END %][% x %][% SET x=x-1 %][% END %]ok",
						vars));
	}

	@Test
	public void testAssign() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("ok", eval("[% x=5 %]ok"));
	}

	@Test
	public void testForNext() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

		assertEquals(
				"124ok",
				eval("[% x=5 %][% FOR x IN [1,2,3,4] %][% IF x==3 %][% NEXT %][% END %][% x %][% x=x-1 %][% END %]ok",
						vars));
	}

	@Test
	public void testConditionalOperator() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("4", eval("[% true ? 4 : 9 %]"));
	}

	@Test
	public void testConditionalOperator2() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("43ok", eval("[% true ? 4 : 9 %][% false ? 5 : 3 %]ok"));
	}

	@Test
	public void testMapAccess() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", ImmutableMap.of("hoge", "fuga"));

		assertEquals("fuga", eval("[% o.hoge %]", vars));
	}

	@Test
	public void testMapLiteral() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<String, Object>();

		assertEquals("fuga",
				eval("[% {hoge=>\"fuga\", gogo=>4649}.hoge %]", vars));
	}

	@Test
	public void testSwitch() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		String src = "[% SWITCH n %][% CASE 1 %]one[% CASE 2 %]two[% CASE %]more[% END %]";
		// System.out.println(new Disassembler().disasm(irep));

		assertEquals("one", eval(src, ImmutableMap.of("n", 1)));
		assertEquals("two", eval(src, ImmutableMap.of("n", 2)));
		assertEquals("more", eval(src, ImmutableMap.of("n", 3)));
		assertEquals("more", eval(src, ImmutableMap.of()));
	}

	@Test
	public void testLowerCase() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("hoge", eval("[% lc(\"HoGe\") %]"));
	}

	@Test
	public void testBuiltinFunctionUri() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("%26+%2B3", eval("[% uri(\"& +3\") %]", vars));
	}

	@Test
	public void testUpperCase() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("HOGE", eval("[% uc(\"HoGe\") %]"));
	}

	@Test
	public void testSprintf() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		Map<String, Object> map = new HashMap<>();
		map.put("hoge", "fuga");

		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("o", map);

		assertEquals("hehe 004, ahaha",
				eval("[% sprintf(\"hehe %03d, %s\", 4, \"ahaha\") %]"));
	}

	@Test
	public void testRange() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("1,2,3,4,5,", eval("[% FOR x IN 1..5 %][% x %],[% END %]"));
	}

	@Test
	public void testNot() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("false,true", eval("[% !true %],[% !false %]"));
	}

	@Test
	public void testPipe() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("hoge%2B%26", eval("[% \"hoge+&\" | uri %]"));
	}

	@Test
	public void testMethodCall() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("4", eval("[% [5,9,6,3].size() %]"));
	}

	@Test
	public void testMethodCall2() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("ge", eval("[% 'hoge'.substring(2) %]"));
	}

	@Test
	public void testMethodCall3() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("urge", eval("[%  'hamburger'.substring(4, 8) %]"));
	}

	@Test
	public void testAndAnd() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true && true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% true && false %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false && true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false && false %]", ImmutableMap.of()));
	}

	@Test
	public void testLooseAnd() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("true", eval("[% true AND true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% true AND false %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false AND true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false AND false %]", ImmutableMap.of()));
	}

	@Test
	public void testOrOr() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("true", eval("[% true || true %]", ImmutableMap.of()));
		assertEquals("true", eval("[% true || false %]", ImmutableMap.of()));
		assertEquals("true", eval("[% false || true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false || false %]", ImmutableMap.of()));
	}

	@Test
	public void testLooseOr() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("true", eval("[% true OR true %]", ImmutableMap.of()));
		assertEquals("true", eval("[% true OR false %]", ImmutableMap.of()));
		assertEquals("true", eval("[% false OR true %]", ImmutableMap.of()));
		assertEquals("false", eval("[% false OR false %]", ImmutableMap.of()));
	}

	@Test
	public void testNE() throws JTTCompilerError, ParserError, IOException,
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
	public void testArrayAccess() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"9",
				eval("[% ary[i] %]",
						ImmutableMap.of("i", 1, "ary",
								Lists.newArrayList(5, 9, 6, 3))));
	}

	@Test
	public void testArrayAccess2() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"9",
				eval("[% ary[$i] %]",
						ImmutableMap.of("i", 1, "ary",
								Lists.newArrayList(5, 9, 6, 3))));
	}

	@Test
	public void testMapIndex() throws JTTCompilerError, ParserError,
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
	public void testArrayDollarVarAccess() throws JTTCompilerError,
			ParserError, IOException, TemplateLoadingError {
		assertEquals(
				"9",
				eval("[% ary.$i %]",
						ImmutableMap.of("i", 1, "ary",
								Lists.newArrayList(5, 9, 6, 3))));
	}

	@Test
	public void testMapDollarVarAccess() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals(
				"fuga",
				eval("[% map.$key %]", ImmutableMap.of("key", "hoge", "map",
						ImmutableMap.of("hoge", "fuga", "hige", "hage"))));
	}

	@Test
	public void testDollarVarAccess() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("4", eval("[% $i %]", ImmutableMap.of("i", 4)));
	}

	@Test
	public void testLoopCount() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("1234",
				eval("[% FOR x IN [5,9,6,3] %][% loop.getCount() %][% END %]"));
	}

	@Test
	public void testLoopHasNext() throws JTTCompilerError, ParserError,
			IOException, TemplateLoadingError {
		assertEquals("truefalse",
				eval("[% FOR x IN [5,9] %][% loop.hasNext() %][% END %]"));
	}

	@Test
	public void testFile() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("-", eval("[% __FILE__ %]"));
	}

	@Test
	public void testLine() throws JTTCompilerError, ParserError, IOException,
			TemplateLoadingError {
		assertEquals("1\n2", eval("[% __LINE__ %]\n[% __LINE__ %]"));
	}

	// ---------------------------------------------------------------------
	//
	// UTILITY FUNCTIONS...
	//
	// ---------------------------------------------------------------------

	private String eval(String src) throws ParserError, JTTCompilerError,
			IOException, TemplateLoadingError {
		return eval(src, new HashMap<String, Object>());
	}

	private String eval(String srcString, Map<String, Object> vars)
			throws ParserError, JTTCompilerError, IOException,
			TemplateLoadingError {
		Source source = Source.fromString(srcString);
		Syntax syntax = new TTSyntax("[%", "%]");
		List<Token> tokens = syntax.tokenize(source, srcString);
		Node ast = syntax.parse(source, tokens);
		Irep irep = syntax.compile(source, ast);
		return new VM(syntax, loader, null, null, new HTMLEscaper(), irep, vars).run();
	}
}
