package me.geso.jtt.tt;

import static org.junit.Assert.*;

import java.util.List;

import me.geso.jtt.lexer.JSlateLexerError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.tt.TTLexerBuilder;

import org.junit.Test;

public class TTLexerTest {
	TTLexerBuilder lexer = new TTLexerBuilder("[%", "%]");

	private String lex(String src) {
		return s(lexer.build(src).lex());
	}

	@Test
	public void testInteger() {
		assertEquals("[INTEGER 3]", lex("[% 3 %]"));
		assertEquals("[INTEGER 0]", lex("[% 0 %]"));
		assertEquals("[INTEGER 30]", lex("[% 30 %]"));
	}

	@Test
	public void testDouble() {
		assertEquals("[DOUBLE 3.14]", lex("[% 3.14 %]"));
	}

	@Test
	public void testForIn() {
		assertEquals("[FOREACH],[IDENT x],[IN],[IDENT y],[IDENT x],[END]",
				lex("[% FOR x IN y %][% x %][% END %]"));
	}

	@Test
	public void testLexer() {
		assertEquals("[RAW hoge]", lex("hoge"));
	}

	@Test
	public void testLexerIdent() {
		assertEquals("[IDENT foo]", lex("[% foo %]"));
	}

	@Test
	public void testIf() {
		assertEquals("[IF],[IDENT foo],[RAW bar],[END]",
				lex("[% IF foo %]bar[% END %]"));
	}

	@Test
	public void testSwitch() {
		assertEquals("[SWITCH],[IDENT foo],[CASE],[INTEGER 3],[RAW bar],[END]",
				lex("[% SWITCH foo %][% CASE 3 %]bar[% END %]"));
	}

	@Test
	public void testArray() {
		assertEquals(
				"[LBRACKET],[INTEGER 1],[COMMA],[INTEGER 2],[COMMA],[INTEGER 3],[RBRACKET]",
				lex("[% [1,2,3] %]"));
	}

	@Test
	public void testEquals() {
		assertEquals("[IDENT x],[EQAULS],[INTEGER 3]", lex("[% x==3 %]"));
	}

	@Test
	public void testAssign() {
		assertEquals("[IDENT x],[ASSIGN],[INTEGER 3]", lex("[% x=3 %]"));
	}

	@Test
	public void testOp() {
		assertEquals("[IDENT x],[MODULO],[INTEGER 3]", lex("[% x%3 %]"));
		assertEquals("[IDENT x],[LT],[INTEGER 3]", lex("[% x<3 %]"));
		assertEquals("[IDENT x],[LE],[INTEGER 3]", lex("[% x<=3 %]"));
		assertEquals("[IDENT x],[GT],[INTEGER 3]", lex("[% x>3 %]"));
		assertEquals("[IDENT x],[GE],[INTEGER 3]", lex("[% x>=3 %]"));
	}

	@Test
	public void testHash() {
		assertEquals("[LBRACE],[IDENT a],[ARROW],[INTEGER 3],[RBRACE]",
				lex("[% {a=>3} %]"));
	}

	@Test
	public void testRaw() {
		assertEquals("[RAW hoge]", lex("hoge"));
	}

	@Test
	public void testConcat() {
		assertEquals("[STRING ho],[CONCAT],[STRING ge]",
				lex("[% \"ho\" _ \"ge\" %]"));
	}

	@Test
	public void testSqString() {
		assertEquals("[STRING ho]", lex("[% 'ho' %]"));
	}

	@Test
	public void testChomp() {
		assertEquals("[RAW hoge],[STRING ho]", lex("hoge\n  [%- 'ho' %]"));
		assertEquals("[STRING ho],[RAW   hoge]", lex("[% 'ho' -%]  \n  hoge"));
	}

	@Test
	public void testParen() {
		assertEquals(
				"[LPAREN],[INTEGER 1],[PLUS],[INTEGER 2],[RPAREN],[MUL],[INTEGER 3]",
				lex("[% (1+2)*3 %]"));
	}

	@Test
	public void testLineComment() {
		assertEquals(
				"[IDENT theta],[ASSIGN],[INTEGER 20],[IDENT rho],[ASSIGN],[INTEGER 30]",
				lex("[% # this is a comment\n"
						+ "  theta = 20      # so is this\n"
						+ "  rho   = 30      # <aol>me too!</aol>\n" + "%]"));
	}

	@Test
	public void testComment() {
		assertEquals(
				"[RAW aaa],[RAW bbb]",
				lex("aaa[%# this is a comment\n"
						+ "  theta = 20      # so is this\n"
						+ "  rho   = 30      # <aol>me too!</aol>\n" + "%]bbb"));
	}

	@Test
	public void testFuncall() {
		assertEquals(
				"[IDENT lc],[LPAREN],[STRING HOGE],[RPAREN]",
				lex("[% lc(\"HOGE\") %]"));
	}

	@Test
	public void testConditionalOperator() {
		assertEquals(
				"[INTEGER 3],[QUESTION],[INTEGER 1],[KOLON],[INTEGER 0]",
				lex("[% 3 ? 1 : 0 %]"));
	}

    @Test
    public void testRangeConstructionOperator() {
        assertEquals(
                "[INTEGER 1],[RANGE],[INTEGER 3]",
                lex("[% 1..3 %]"));
    }

    @Test
    public void testUnaryNot() {
        assertEquals(
                "[NOT],[TRUE]",
                lex("[% !true %]"));
    }

	@Test
	public void testError() {
		JSlateLexerError e = null;
		try {
			lexer.build("[%").lex();
		} catch (JSlateLexerError err) {
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
