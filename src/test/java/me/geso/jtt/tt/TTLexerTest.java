package me.geso.jtt.tt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import me.geso.jtt.lexer.Token;

import org.junit.Test;

public class TTLexerTest {
	private String lex(String src) {
		TTLexer lexer = new TTLexer("-", src, "[%", "%]");
		return s(lexer.lex());
	}

	@Test
	public void testInteger() {
		assertEquals("[OPEN],[INTEGER 3],[CLOSE]", lex("[% 3 %]"));
		assertEquals("[OPEN],[INTEGER 0],[CLOSE]", lex("[% 0 %]"));
		assertEquals("[OPEN],[INTEGER 30],[CLOSE]", lex("[% 30 %]"));
	}

	@Test
	public void testDouble() {
		assertEquals("[OPEN],[DOUBLE 3.14],[CLOSE]", lex("[% 3.14 %]"));
	}

	@Test
	public void testForIn() {
		assertEquals("[OPEN],[FOREACH],[IDENT x],[IN],[IDENT y],[CLOSE],[OPEN],[IDENT x],[CLOSE],[OPEN],[END],[CLOSE]",
				lex("[% FOR x IN y %][% x %][% END %]"));
	}

	@Test
	public void testLexer() {
		assertEquals("[RAW hoge]", lex("hoge"));
	}

	@Test
	public void testLexerIdent() {
		assertEquals("[OPEN],[IDENT foo],[CLOSE]", lex("[% foo %]"));
	}

	@Test
	public void testIf() {
		assertEquals("[OPEN],[IF],[IDENT foo],[CLOSE],[RAW bar],[OPEN],[END],[CLOSE]",
				lex("[% IF foo %]bar[% END %]"));
	}

	@Test
	public void testSwitch() {
		assertEquals("[OPEN],[SWITCH],[IDENT foo],[CLOSE],[OPEN],[CASE],[INTEGER 3],[CLOSE],[RAW bar],[OPEN],[END],[CLOSE]",
				lex("[% SWITCH foo %][% CASE 3 %]bar[% END %]"));
	}

	@Test
	public void testArray() {
		assertEquals(
				"[OPEN],[LBRACKET],[INTEGER 1],[COMMA],[INTEGER 2],[COMMA],[INTEGER 3],[RBRACKET],[CLOSE]",
				lex("[% [1,2,3] %]"));
	}

	@Test
	public void testEquals() {
		assertEquals("[OPEN],[IDENT x],[EQUALS],[INTEGER 3],[CLOSE]", lex("[% x==3 %]"));
	}

	@Test
	public void testAssign() {
		assertEquals("[OPEN],[IDENT x],[ASSIGN],[INTEGER 3],[CLOSE]", lex("[% x=3 %]"));
	}

	@Test
	public void testOp() {
		assertEquals("[OPEN],[IDENT x],[MODULO],[INTEGER 3],[CLOSE]", lex("[% x%3 %]"));
		assertEquals("[OPEN],[IDENT x],[LT],[INTEGER 3],[CLOSE]", lex("[% x<3 %]"));
		assertEquals("[OPEN],[IDENT x],[LE],[INTEGER 3],[CLOSE]", lex("[% x<=3 %]"));
		assertEquals("[OPEN],[IDENT x],[GT],[INTEGER 3],[CLOSE]", lex("[% x>3 %]"));
		assertEquals("[OPEN],[IDENT x],[GE],[INTEGER 3],[CLOSE]", lex("[% x>=3 %]"));
	}

	@Test
	public void testHash() {
		assertEquals("[OPEN],[LBRACE],[IDENT a],[ARROW],[INTEGER 3],[RBRACE],[CLOSE]",
				lex("[% {a=>3} %]"));
	}

	@Test
	public void testRaw() {
		assertEquals("[RAW hoge]", lex("hoge"));
	}

	@Test
	public void testConcat() {
		assertEquals("[OPEN],[STRING ho],[CONCAT],[STRING ge],[CLOSE]",
				lex("[% \"ho\" _ \"ge\" %]"));
	}

	@Test
	public void testSqString() {
		assertEquals("[OPEN],[STRING ho],[CLOSE]", lex("[% 'ho' %]"));
	}

	@Test
	public void testChomp() {
		assertEquals("[RAW hoge],[OPEN],[STRING ho],[CLOSE]", lex("hoge\n  [%- 'ho' %]"));
		assertEquals("[OPEN],[STRING ho],[CLOSE],[RAW   hoge]", lex("[% 'ho' -%]  \n  hoge"));
	}

	@Test
	public void testParen() {
		assertEquals(
				"[OPEN],[LPAREN],[INTEGER 1],[PLUS],[INTEGER 2],[RPAREN],[MUL],[INTEGER 3],[CLOSE]",
				lex("[% (1+2)*3 %]"));
	}

	@Test
	public void testLineComment() {
		assertEquals(
				"[OPEN],[IDENT theta],[ASSIGN],[INTEGER 20],[IDENT rho],[ASSIGN],[INTEGER 30],[CLOSE]",
				lex("[% # this is a comment\n"
						+ "  theta = 20      # so is this\n"
						+ "  rho   = 30      # <aol>me too!</aol>\n" + "%]"));
	}

	@Test
	public void testComment() {
		assertEquals("[RAW aaa],[RAW bbb]", lex("aaa[%# this is a comment\n"
				+ "  theta = 20      # so is this\n"
				+ "  rho   = 30      # <aol>me too!</aol>\n" + "%]bbb"));
	}

	@Test
	public void testFuncall() {
		assertEquals("[OPEN],[IDENT lc],[LPAREN],[STRING HOGE],[RPAREN],[CLOSE]",
				lex("[% lc(\"HOGE\") %]"));
	}

	@Test
	public void testConditionalOperator() {
		assertEquals("[OPEN],[INTEGER 3],[QUESTION],[INTEGER 1],[KOLON],[INTEGER 0],[CLOSE]",
				lex("[% 3 ? 1 : 0 %]"));
	}

	@Test
	public void testRangeConstructionOperator() {
		assertEquals("[OPEN],[INTEGER 1],[RANGE],[INTEGER 3],[CLOSE]", lex("[% 1..3 %]"));
	}

	@Test
	public void testUnaryNot() {
		assertEquals("[OPEN],[NOT],[TRUE],[CLOSE]", lex("[% !true %]"));
	}

	@Test
	public void testPipe() {
		assertEquals("[OPEN],[INTEGER 3],[PIPE],[IDENT uri],[CLOSE]", lex("[% 3 | uri %]"));
	}

	@Test
	public void testAnd() {
		assertEquals("[OPEN],[INTEGER 3],[ANDAND],[INTEGER 4],[CLOSE]", lex("[% 3 && 4 %]"));
	}

	@Test
	public void testNE() {
		assertEquals("[OPEN],[INTEGER 3],[NE],[INTEGER 4],[CLOSE]", lex("[% 3 != 4 %]"));
	}

	@Test
	public void testOrOr() {
		assertEquals("[OPEN],[INTEGER 3],[OROR],[INTEGER 4],[CLOSE]", lex("[% 3 || 4 %]"));
	}

	@Test
	public void testLooseAnd() {
		assertEquals("[OPEN],[INTEGER 3],[LOOSE_AND],[INTEGER 4],[CLOSE]",
				lex("[% 3 AND 4 %]"));
	}

	@Test
	public void testLooseOr() {
		assertEquals("[OPEN],[INTEGER 3],[LOOSE_OR],[INTEGER 4],[CLOSE]", lex("[% 3 OR 4 %]"));
	}

	@Test
	public void testDollarVar() {
		assertEquals("[OPEN],[IDENT list],[DOT],[DOLLARVAR key],[CLOSE]",
				lex("[% list.$key %]"));
	}

	@Test
	public void testArrayIndex() {
		assertEquals("[OPEN],[IDENT list],[LBRACKET],[IDENT idx],[RBRACKET],[CLOSE]",
				lex("[% list[idx] %]"));
	}

	@Test
	public void testFile() {
		assertEquals("[OPEN],[FILE],[CLOSE]", lex("[% __FILE__ %]"));
		
		// MUST NOT BE [FILE],[IDENT e]
		assertEquals("[OPEN],[CONCAT],[CONCAT],[IDENT FILE__e],[CLOSE]", lex("[% __FILE__e %]"));
	}

	@Test
	public void testLine() {
		assertEquals("[OPEN],[LINE],[CLOSE]", lex("[% __LINE__ %]"));
		assertNotEquals("[OPEN],[LIKE],[IDENT e],[CLOSE]", lex("[% __FILE__e %]"));
	}

	@Test
	public void testError() {
		TTLexerError e = null;
		try {
			new TTLexer("-", "[%", "[%", "%]").lex();
		} catch (TTLexerError err) {
			e = err;
		}
		assertNotNull(e);
	}

	private String s(List<Token> tokens) {
		StringBuilder builder = new StringBuilder();
		for (Token token : tokens) {
			if (token.getString() != null) {
				builder.append(String.format(",[%s %s]", token.getType(),
						token.getString()));
			} else {
				builder.append(String.format(",[%s]", token.getType()));
			}
		}
		if (builder.length() > 1) {
			return new String(builder).substring(1);
		} else {
			return "";
		}
	}
}
