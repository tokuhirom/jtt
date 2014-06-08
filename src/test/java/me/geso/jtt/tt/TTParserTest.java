package me.geso.jtt.tt;

import static org.junit.Assert.assertEquals;

import java.util.List;

import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.parser.ParserError;
import me.geso.jtt.tt.TTSyntax;

import org.junit.Test;

public class TTParserTest {

	@Test
	public void testInt0() throws ParserError {
		Node node = parse("[% 0 %]");
		assertEquals("(template (expression (integer 0)))", node.toString());
	}

	@Test
	public void testTag() throws ParserError {
		Node node = parse("[% 5963 %]");
		assertEquals("(template (expression (integer 5963)))", node.toString());
	}

	@Test
	public void testTagInt00() throws ParserError {
		Node node = parse("[% 5900 %]");
		assertEquals("(template (expression (integer 5900)))", node.toString());
	}

	@Test
	public void testRawString() throws ParserError {
		Node node = parse("hoge");
		assertEquals("(template (raw_string hoge))", node.toString());
	}

	@Test
	public void testRawString2() throws ParserError {
		Node node = parse("ho[ge");
		assertEquals("(template (raw_string ho[ge))", node.toString());
	}

	@Test
	public void testAdditive() throws ParserError {
		Node node = parse("[% 3 + 4 %]");
		assertEquals("(template (expression (add (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testMultiply() throws ParserError {
		Node node = parse("[% 3 * 4 %]");

		assertEquals(NodeType.TEMPLATE, node.getType());
		assertEquals(1, node.getChildren().size());
		assertEquals(NodeType.EXPRESSION, node.getChildren().get(0).getType());
		assertEquals(1, node.getChildren().get(0).getChildren().size());
		assertEquals(NodeType.MULTIPLY, node.getChildren().get(0).getChildren()
				.get(0).getType());
		assertEquals(2, node.getChildren().get(0).getChildren().get(0)
				.getChildren().size());
	}

	@Test
	public void testIdent() throws ParserError {
		Node node = parse("[% abc %]");
		assertEquals("(template (expression (ident abc)))", node.toString());
	}

	@Test
	public void testExpr2() throws ParserError {
		Node node = parse("a[% 1 %][% 2 %]");

		assertEquals(NodeType.TEMPLATE, node.getType());
		assertEquals(3, node.getChildren().size());
		assertEquals(NodeType.RAW_STRING, node.getChildren().get(0).getType());
		assertEquals(NodeType.EXPRESSION, node.getChildren().get(1).getType());
		assertEquals(NodeType.EXPRESSION, node.getChildren().get(2).getType());
	}

	@Test
	public void testForEach() throws ParserError {
		Node node = parse("[% FOR x IN y %][% x %][% END %]");

		assertEquals(
				"(template (foreach (ident x) (ident y) (template (expression (ident x)))))",
				node.toString());
	}

	@Test
	public void testEquals() throws ParserError {
		Node node = parse("[% 3 == 4 %]");

		assertEquals(
				"(template (expression (eqauls (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testGt() throws ParserError {
		Node node = parse("[% 3 > 4 %]");

		assertEquals("(template (expression (gt (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testGE() throws ParserError {
		Node node = parse("[% 3 >= 4 %]");

		assertEquals("(template (expression (ge (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testLT() throws ParserError {
		Node node = parse("[% 3 < 4 %]");

		assertEquals("(template (expression (lt (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testLE() throws ParserError {
		Node node = parse("[% 3 <= 4 %]");

		assertEquals("(template (expression (le (integer 3) (integer 4))))",
				node.toString());
	}

	@Test
	public void testArray() throws ParserError {
		Node node = parse("[% [1,2,3] %]");

		assertEquals(
				"(template (expression (array (integer 1) (integer 2) (integer 3))))",
				node.toString());
	}

	@Test
	public void testParen() throws ParserError {
		Node node = parse("[% (1+2)*3 %]");

		assertEquals(
				"(template (expression (multiply (add (integer 1) (integer 2)) (integer 3))))",
				node.toString());
	}

	@Test
	public void testMod() throws ParserError {
		Node node = parse("[% 4 % 2 %]");

		assertEquals(
				"(template (expression (modulo (integer 4) (integer 2))))",
				node.toString());
	}

	@Test
	public void testIf() throws ParserError {
		Node node = parse("[% IF 1 %]3[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (if (integer 1) (template (raw_string 3)) null))",
				node.toString());
	}

	@Test
	public void testIfElsIf() throws ParserError {
		Node node = parse("[% IF 1 %]3[% ELSIF 4 %]f[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (if (integer 1) (template (raw_string 3)) (if (integer 4) (template (raw_string f)) null)))",
				node.toString());
	}

	@Test
	public void testIfElse() throws ParserError {
		Node node = parse("[% IF 1 %]3[% ELSE %]f[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (if (integer 1) (template (raw_string 3)) (template (raw_string f))))",
				node.toString());
	}

	@Test
	public void testIfElsIfElse() throws ParserError {
		Node node = parse("[% IF 1 %]3[% ELSIF x %]y[% ELSE %]f[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (if (integer 1) (template (raw_string 3)) (if (ident x) (template (raw_string y)) (template (raw_string f)))))",
				node.toString());
	}

	@Test
	public void testTrue() throws ParserError {
		Node node = parse("[% true %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (expression (true)))", node.toString());
	}

	@Test
	public void testFalse() throws ParserError {
		Node node = parse("[% false %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (expression (false)))", node.toString());
	}

	@Test
	public void testNull() throws ParserError {
		Node node = parse("[% null %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (expression (null)))", node.toString());
	}

	@Test
	public void testString() throws ParserError {
		Node node = parse("[% \"hoge\\\\\\\"\" %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (expression (string hoge\\\")))",
				node.toString());
	}

	@Test
	public void testConcat() throws ParserError {
		Node node = parse("[% \"ho\" _ \"ge\" %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (expression (concat (string ho) (string ge))))",
				node.toString());
	}

	@Test
	public void testSet() throws ParserError {
		Node node = parse("[% SET x=3 %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (set (ident x) (integer 3)))", node.toString());
	}

	@Test
	public void testAssign() throws ParserError {
		Node node = parse("[% x=3 %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (expression (set (ident x) (integer 3))))", node.toString());
	}

	@Test
	public void testWhile() throws ParserError {
		Node node = parse("[% WHILE x %]y[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (while (ident x) (template (raw_string y))))",
				node.toString());
	}

	@Test
	public void testWhileExpression() throws ParserError {
		Node node = parse("[% WHILE x > 3 %]y[% END %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals(
				"(template (while (gt (ident x) (integer 3)) (template (raw_string y))))",
				node.toString());
	}

	@Test
	public void testLast() throws ParserError {
		Node node = parse("[% LAST %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (last))", node.toString());
	}

	@Test
	public void testNext() throws ParserError {
		Node node = parse("[% NEXT %]");

		// (template (if (integer 1) (template (raw_string %]3)) null))
		assertEquals("(template (next))", node.toString());
	}

	@Test
	public void testSingleQuoteString() throws ParserError {
		Node node = parse("[% 'hoge' %]");

		assertEquals("(template (expression (string hoge)))", node.toString());
	}

	@Test
	public void testInclude() throws ParserError {
		Node node = parse("[% INCLUDE 'hoge' %]");

		assertEquals("(template (include (string hoge)))", node.toString());
	}

	@Test
	public void testHash() throws ParserError {
		Node node = parse("[% {a=>b} %]");
		assertEquals("(template (expression (map (ident a) (ident b))))",
				node.toString());
	}

	@Test
	public void testHashComma() throws ParserError {
		Node node = parse("[% {a=>b,} %]");
		assertEquals("(template (expression (map (ident a) (ident b))))",
				node.toString());
	}

	@Test
	public void testHashCommaHash() throws ParserError {
		Node node = parse("[% {a=>b,\"c\"=>d} %]");
		assertEquals(
				"(template (expression (map (ident a) (ident b) (string c) (ident d))))",
				node.toString());
	}

	@Test
	public void testHashAccess() throws ParserError {
		Node node = parse("[% hoge.fuga %]");
		assertEquals(
				"(template (expression (attribute (ident hoge) (ident fuga))))",
				node.toString());
	}

	@Test
	public void testSwitch() throws ParserError {
		Node node = parse("[% SWITCH x %][% CASE 1 %]a[% CASE 2 %]b[% CASE %]c[% END %]");
		assertEquals(
				"(template (switch (ident x) (case (integer 1) (template (raw_string a))) (case (integer 2) (template (raw_string b))) (case null (template (raw_string c)))))",
				node.toString());
	}

	@Test
	public void testFuncall() throws ParserError {
		Node node = parse("[% foo() %]");
		assertEquals(
				"(template (expression (funcall (ident foo))))",
				node.toString());
	}

	@Test
	public void testFuncall2() throws ParserError {
		Node node = parse("[% foo(a, b) %]");
		assertEquals(
				"(template (expression (funcall (ident foo) (ident a) (ident b))))",
				node.toString());
	}


	@Test
	public void testFuncall3() throws ParserError {
		Node node = parse("[% foo.bar(a, b) %]");
		assertEquals(
				"(template (expression (funcall (attribute (ident foo) (ident bar)) (ident a) (ident b))))",
				node.toString());
	}

	@Test
	public void testConditionalOperator() throws ParserError {
		Node node = parse("[% 3 ? 1 : 4 %]");
		assertEquals(
				"(template (expression (if (integer 3) (integer 1) (integer 4))))",
				node.toString());
	}

    @Test
    public void testRangeOperator() throws ParserError {
        Node node = parse("[% 1..3 %]");
        assertEquals(
                "(template (expression (range (integer 3) (integer 1))))",
                node.toString());
    }

    @Test
    public void testNot() throws ParserError {
        Node node = parse("[% !true %]");
        assertEquals(
                "(template (expression (not (true))))",
                node.toString());
    }

    @Test
    public void testPipe() throws ParserError {
        Node node = parse("[% 5963 | uri %]");
        assertEquals(
                "(template (expression (funcall (ident uri) (integer 5963))))",
                node.toString());
    }

    @Test
    public void testMethodSize() throws ParserError {
        Node node = parse("[% [1,2,3].size() %]");
        assertEquals(
                "(template (expression (funcall (attribute (array (integer 1) (integer 2) (integer 3)) (ident size)))))",
                node.toString());
    }

    @Test
    public void testAndAnd() throws ParserError {
        Node node = parse("[% true && false %]");
        assertEquals(
                "(template (expression (andand (true) (false))))",
                node.toString());
    }

    // left AND
    @Test
    public void testLooseAnd() throws ParserError {
        assertEquals(
                "(template (expression (andand (true) (false))))",
                parse("[% true AND false %]").toString());
        assertEquals(
                "(template (expression (andand (andand (true) (false)) (true))))",
                parse("[% true AND false AND true %]").toString());
    }

    @Test
    public void testOrOr() throws ParserError {
        Node node = parse("[% true || false %]");
        assertEquals(
                "(template (expression (oror (true) (false))))",
                node.toString());
    }

    @Test
    public void testLooseOr() throws ParserError {
        Node node = parse("[% true OR false %]");
        assertEquals(
                "(template (expression (oror (true) (false))))",
                node.toString());
    }

    @Test
    public void testNE() throws ParserError {
        Node node = parse("[% true != false %]");
        assertEquals(
                "(template (expression (ne (true) (false))))",
                node.toString());
    }

    @Test
    public void testDollarVar() throws ParserError {
        Node node = parse("[% list.$var %]");
        assertEquals(
                "(template (expression (attribute (ident list) (dollarvar var))))",
                node.toString());
    }

	private Node parse(String source) throws ParserError {
		TTSyntax syntax = new TTSyntax("[%", "%]");
		List<Token> tokens = syntax.tokenize(source);
		Node node = syntax.parse(source, tokens);
		return node;
	}
}
