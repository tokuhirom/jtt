package me.geso.jtt.tt;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import me.geso.jtt.Source;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.lexer.TokenType;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.parser.Parser;

import com.google.common.collect.Lists;

class TTParser implements Parser {
	private int pos = 0;
	private Source source;
	private List<Token> tokens;

	@Override
	public Source getSource() {
		return this.source;
	}

	@Override
	public int getLine() {
		return this.tokens.get(pos).getLineNumber();
	}

	public TTParser(Source source, List<Token> tokens) {
		this.source = source;
		this.tokens = tokens;
	}

	public Node parseTemplate() throws ParserError {
		Node n = parseTemplateBody();
		if (n == null) {
			throw new ParserError("Can't parse template", this);
		}
		if (this.pos != tokens.size()) {
			throw new ParserError("Can't parse template", this);
		}
		return n;
	}

	public Node parseTemplateBody() throws ParserError {
		List<Node> children = new ArrayList<>();
		while (pos < tokens.size()) {
			Node rawStringNode = parseRawString();
			if (rawStringNode != null) {
				children.add(rawStringNode);
				continue;
			}

			Node tagNode = parseTag();
			if (tagNode != null) {
				children.add(tagNode);
				continue;
			}

			break;
		}
		return new Node(NodeType.TEMPLATE, children, PREV_LINE_NUMBER());
	}

	Node parseRawString() {
		if (CURRENT_TYPE() == TokenType.RAW) {
			Node node = new Node(NodeType.RAW_STRING, CURRENT_STRING(),
					PREV_LINE_NUMBER());
			++pos;
			return node;
		} else {
			return null;
		}
	}

	// tag : '[%' expr '%]';
	public Node parseTag() throws ParserError {
		if (CURRENT_TYPE() != TokenType.OPEN) {
			return null;
		}
		if (!HAS_NEXT_TOKEN()) {
			throw new ParserError("Missing token after opening tag", this);
		}
		switch (NEXT_TYPE()) {
		// switch (tokens.get(pos).getType()) {
		case FOREACH: {
			return parseForEach();
		}
		case WHILE:
			return parseWhile();
		case LAST: {
			return parseLast();
		}
		case NEXT: {
			return parseNext();
		}
		case IF:
			return parseIf();
		case INCLUDE:
			return parseInclude();
		case SET:
			return parseSet();
		case SWITCH:
			return parseSwitch();
		case WRAPPER:
			return parseWrapper();
		case END:
		case ELSE:
		case ELSIF:
		case CASE:
			// Unexpected [% END %] etc... Backtracking is required.
			return null;
		default: {
			return this.parseTagExpr();
		}
		}
	}

	private Node parseForEach() {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.FOREACH)) {
			return null;
		}

		Node iterNode = parseIdent();
		if (iterNode == null) {
			throw new ParserError("No variable name after (FOR|FOREACH)", this);
		}

		if (!EAT(TokenType.IN)) {
			throw new ParserError("No 'IN' keyword after (FOR|FOREACH)", this);
		}

		Node expr = parseExpr();
		if (expr == null) {
			throw new ParserError("No expression after (FOR|FOREACH)", this);
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after (FOR|FOREACH)",
					this);
		}

		Node body = parseTemplateBody();
		if (body == null) {
			throw new ParserError("No template body after (FOR|FOREACH)", this);
		}

		if (!parseEnd()) {
			throw new ParserError("No `[% END %]` after (FOR|FOREACH)", this);
		}

		List<Node> children = new ArrayList<>();
		children.add(iterNode);
		children.add(expr);
		children.add(body);
		return new Node(NodeType.FOREACH, children, PREV_LINE_NUMBER());
	}

	private Node parseLast() {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.LAST)) {
			return null;
		}

		Node node = new Node(NodeType.LAST, PREV_LINE_NUMBER());

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after LAST", this);
		}

		return node;
	}

	private Node parseNext() {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.NEXT)) {
			return null;
		}

		Node node = new Node(NodeType.NEXT, PREV_LINE_NUMBER());

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after NEXT", this);
		}

		return node;
	}

	private Node parseTagExpr() {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}
		Node exprNode = parseExpr();
		if (exprNode == null) {
			throw new ParserError("No expression", this);
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after expression", this);
		}

		return new Node(NodeType.EXPRESSION, exprNode, PREV_LINE_NUMBER());
	}

	// [% WRAPPER "foo.tt" %]body[% END %]
	private Node parseWrapper() {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}
		if (!EAT(TokenType.WRAPPER)) {
			return null;
		}

		int lineNumber = CURRENT_LINE_NUMBER();

		Node fileName = parseExpr();
		if (fileName == null) {
			throw new ParserError("Missing file name after WRAPPER keyword.",
					this);
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after WRAPPER keyword.",
					this);
		}

		Node body = parseTemplateBody();

		if (!parseEnd()) {
			throw new ParserError("Missing END after WRAPPER keyword.", this);
		}

		return new Node(NodeType.WRAPPER, Lists.newArrayList(fileName, body),
				lineNumber);
	}

	private Node parseSwitch() throws ParserError {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}
		if (!EAT(TokenType.SWITCH)) {
			return null;
		}

		List<Node> list = new ArrayList<Node>();

		Node expr = parseExpr();
		if (expr == null) {
			throw new ParserError("Missing expression after SWITCH", this);
		}
		list.add(expr);

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after SWITCH", this);
		}

		while (true) {
			Node caseNode = parseCase();
			if (caseNode != null) {
				list.add(caseNode);
				continue;
			}

			break;
		}

		if (!parseEnd()) {
			throw new ParserError("Missing END tag after SWITCH", this);
		}

		return new Node(NodeType.SWITCH, list, PREV_LINE_NUMBER());
	}

	// [% CASE expr %]
	private Node parseCase() throws ParserError {
		try (PositionSaver saver = new PositionSaver(this)) {
			if (!EAT(TokenType.OPEN)) {
				return null;
			}
			if (!EAT(TokenType.CASE)) {
				return null;
			}

			// expr maybe null. `[% CASE %]` is valid.
			Node expr = parseExpr();

			if (!EAT(TokenType.CLOSE)) {
				throw new ParserError("Missing closing tag after CASE", this);
			}

			Node body = parseTemplateBody();
			if (body == null) {
				throw new ParserError("Missing templates after CASE", this);
			}

			saver.commit();
			return new Node(NodeType.CASE, Lists.newArrayList(expr, body),
					PREV_LINE_NUMBER());
		}
	}

	// [% INCLUDE "hello.tt" %]
	// [% INCLUDE "hello.tt" WITH foo %]
	private Node parseInclude() throws ParserError {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.INCLUDE)) {
			throw new ParserError("No 'INCLUDE'", this);
		}

		Node path = parseString();

		ArrayList<Node> args = new ArrayList<>();
		args.add(path);

		if (EAT(TokenType.WITH)) {
			while (CURRENT_TYPE() != TokenType.CLOSE) {
				Node ident = parseIdent();
				if (ident == null) {
					throw new ParserError("Missing ident after WITH", this);
				}
				args.add(ident);

				if (!EAT(TokenType.ASSIGN)) {
					throw new ParserError("Missing '=' after WITH", this);
				}

				Node expr = parseExpr();
				if (expr == null) {
					throw new ParserError("Missing expr after WITH: "
							+ CURRENT_TYPE(), this);
				}
				args.add(expr);

				if (EAT(TokenType.COMMA)) {
					continue;
				} else {
					break;
				}
			}
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after 'INCLUDE'", this);
		}

		return new Node(NodeType.INCLUDE, args, PREV_LINE_NUMBER());
	}

	private Node parseWhile() throws ParserError {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.WHILE)) {
			throw new ParserError("No 'WHILE'", this);
		}

		final Node expr = parseExpr();
		if (expr == null) {
			throw new ParserError("No expression after 'WHILE'", this);
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after 'WHILE'", this);
		}

		final Node body = parseTemplateBody();

		if (!parseEnd()) {
			throw new ParserError("No END tag after 'IF'", this);
		}

		return new Node(NodeType.WHILE, Lists.newArrayList(expr, body),
				PREV_LINE_NUMBER());
	}

	private Node parseSet() throws ParserError {
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.SET)) {
			throw new ParserError("No 'SET'", this);
		}

		final Node ident = parseIdent();
		if (!EAT(TokenType.ASSIGN)) {
			throw new ParserError("No = after 'SET'", this);
		}
		final Node expr = parseExpr();

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after 'SET'", this);
		}

		return new Node(NodeType.SET, Lists.newArrayList(ident, expr),
				PREV_LINE_NUMBER());
	}

	private Node parseIf() throws ParserError {
		// (if condition if-body else-body)
		if (!EAT(TokenType.OPEN)) {
			return null;
		}

		if (!EAT(TokenType.IF)) {
			throw new ParserError("No 'IF'", this);
		}

		final Node cond = parseExpr();
		if (cond == null) {
			throw new ParserError("No condition after 'IF'", this);
		}

		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag after 'IF'", this);
		}

		final Node body = parseTemplateBody();
		if (body == null) {
			throw new ParserError("No body after 'IF'", this);
		}

		Node elseBody = parseElsIf();
		if (elseBody == null) {
			elseBody = parseElse();
		}

		if (!parseEnd()) {
			throw new ParserError("No `[% END %]` after 'IF'", this);
		}

		List<Node> children = new ArrayList<>();
		children.add(cond);
		children.add(body);
		children.add(elseBody);

		return new Node(NodeType.IF, children, PREV_LINE_NUMBER());
	}

	private Node parseElsIf() throws ParserError {
		if (NEXT_TYPE() != TokenType.ELSIF) {
			return null;
		}

		if (!EAT(TokenType.OPEN)) {
			throw new ParserError("Missing open tag before 'ELSIF'", this);
		}
		if (!EAT(TokenType.ELSIF)) {
			throw new ParserError("Should not reach here", this);
		}

		final Node elsifCond = parseExpr();
		if (elsifCond == null) {
			throw new ParserError("No condition after 'ELSIF'", this);
		}
		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("No closing tag after 'ELSIF'", this);
		}

		Node elsifBody = parseTemplateBody();
		if (elsifBody == null) {
			throw new ParserError("No body after 'ELSIF'", this);
		}
		Arrays.asList(elsifCond, elsifBody);
		Node elseClause = parseElsIf();
		if (elseClause == null) {
			elseClause = parseElse();
		}

		List<Node> list = Arrays.asList(elsifCond, elsifBody, elseClause);
		return new Node(NodeType.IF, list, PREV_LINE_NUMBER());
	}

	private Node parseElse() throws ParserError {
		if (NEXT_TYPE() != TokenType.ELSE) {
			return null;
		}

		if (!EAT(TokenType.OPEN)) {
			throw new ParserError("Unknown parser error around ELSE keyword",
					this);
		}
		if (!EAT(TokenType.ELSE)) {
			throw new ParserError("Unknown parser error around ELSE keyword",
					this);
		}
		if (!EAT(TokenType.CLOSE)) {
			throw new ParserError("Missing closing tag around ELSE keyword",
					this);
		}

		Node elseBody = parseTemplateBody();
		if (elseBody == null) {
			throw new ParserError("No body after 'ELSE'", this);
		}
		return elseBody;
	}

	class PositionSaver implements Closeable {
		final TTParser parser;
		final int pos;
		boolean committed = false;

		public PositionSaver(TTParser parser) {
			this.parser = parser;
			this.pos = parser.getPos();
		}

		public void commit() {
			committed = true;
		}

		public void close() {
			if (!committed) {
				this.parser.setPos(this.pos);
			}
		}
	}

	private boolean parseEnd() {
		if (!EAT(TokenType.OPEN)) {
			return false;
		}

		if (!EAT(TokenType.END)) {
			return false;
		}

		if (!EAT(TokenType.CLOSE)) {
			return false;
		}

		return true;
	}

	int getPos() {
		return this.pos;
	}

	public String getFileName() {
		return CURRENT_FILENAME();
	}

	// expr = int;
	// expr = int '+' int;
	public Node parseExpr() throws ParserError {
		Node n = parsePipe();
		if (n != null) {
			return n;
		} else {
			return null;
		}
	}

	// pipe = assign '|' assign;
	public Node parsePipe() throws ParserError {
		Node n = parseLooseOr();
		while (EAT(TokenType.PIPE)) {
			Node lhs = parseLooseOr();
			if (lhs == null) {
				throw new ParserError("Missing expression after '|'", this);
			}
			if (lhs.getType() == NodeType.IDENT) {
				n = new Node(NodeType.FUNCALL, lhs, n, PREV_LINE_NUMBER());
			} else {
				throw new ParserError("left side of pipe must be ident", this);
			}
		}
		return n;
	}

	public Node parseLooseOr() throws ParserError {
		Node n = parseLooseAnd();
		if (n != null) {
			while (true) {
				if (EAT(TokenType.LOOSE_OR)) {
					Node rhs = parseLooseAnd();
					if (rhs == null) {
						throw new ParserError(
								"Missing expression after 'OR' : "
										+ CURRENT_TYPE(), this);
					}
					n = new Node(NodeType.OROR, n, rhs, PREV_LINE_NUMBER());
				} else {
					break;
				}
			}
		}
		return n;
	}

	// left AND
	public Node parseLooseAnd() throws ParserError {
		Node n = parseAssign();
		if (n != null) {
			while (true) {
				if (EAT(TokenType.LOOSE_AND)) {
					Node rhs = parseAssign();
					if (rhs == null) {
						throw new ParserError(
								"Missing expression after 'AND' : "
										+ CURRENT_TYPE(), this);
					}
					n = new Node(NodeType.ANDAND, n, rhs, PREV_LINE_NUMBER());
				} else {
					break;
				}
			}
		}
		return n;
	}

	public Node parseAssign() throws ParserError {
		Node n = parseConditionalOperator();
		if (n != null) {
			if (EAT(TokenType.ASSIGN)) {
				Node lhs = parseConditionalOperator();
				if (lhs == null) {
					throw new ParserError("Missing expression after '=' : "
							+ CURRENT_TYPE(), this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(lhs);
				n = new Node(NodeType.SET, children, PREV_LINE_NUMBER());
			}
			return n;
		} else {
			return null;
		}
	}

	public Node parseConditionalOperator() throws ParserError {
		Node n = parseRangeExpr();
		if (n != null) {
			if (EAT(TokenType.QUESTION)) {
				Node c = parseRangeExpr();
				if (c == null) {
					throw new ParserError("Missing expression after '?' : "
							+ CURRENT_TYPE(), this);
				}

				if (!EAT(TokenType.KOLON)) {
					throw new ParserError("Missing ':' after '?' : "
							+ CURRENT_TYPE(), this);
				}
				Node l = parseRangeExpr();
				if (l == null) {
					throw new ParserError("Missing expression after ':' : "
							+ CURRENT_TYPE(), this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(c);
				children.add(l);
				n = new Node(NodeType.IF, children, PREV_LINE_NUMBER());
			}
		}
		return n;
	}

	public Node parseRangeExpr() throws ParserError {
		Node n = parseOrOr();
		if (n != null) {
			if (EAT(TokenType.RANGE)) {
				Node l = parseOrOr();
				if (l == null) {
					throw new ParserError("Missing expression after ':' : "
							+ CURRENT_TYPE(), this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(l);
				n = new Node(NodeType.RANGE, children, PREV_LINE_NUMBER());
			}
		}
		return n;
	}

	public Node parseOrOr() throws ParserError {
		Node n = parseAndAnd();
		if (n != null) {
			while (true) {
				if (EAT(TokenType.OROR)) {
					Node rhs = parseAndAnd();
					if (rhs == null) {
						throw new ParserError("Missing expression after '||': "
								+ CURRENT_TYPE(), this);
					}
					n = new Node(NodeType.OROR, n, rhs, PREV_LINE_NUMBER());
				} else {
					break;
				}
			}
		}
		return n;
	}

	public Node parseAndAnd() throws ParserError {
		Node n = parseEqualityExpr();
		if (n != null) {
			while (true) {
				if (EAT(TokenType.ANDAND)) {
					Node rhs = parseEqualityExpr();
					if (rhs == null) {
						throw new ParserError("Missing expression after '&&': "
								+ CURRENT_TYPE(), this);
					}
					n = new Node(NodeType.ANDAND, n, rhs, PREV_LINE_NUMBER());
				} else {
					break;
				}
			}
		}
		return n;
	}

	public Node parseEqualityExpr() throws ParserError {
		Node n = parseComparationExpr();
		if (n != null) {
			while (true) {
				// ==
				if (EAT(TokenType.EQUALS)) {
					Node rhs = parseComparationExpr();
					if (rhs == null) {
						throw new ParserError(
								"Missing additive expression after '=='", this);
					}

					List<Node> children = new ArrayList<>();
					children.add(n);
					children.add(rhs);
					n = new Node(NodeType.EQUALS, children, PREV_LINE_NUMBER());
				} else if (EAT(TokenType.NE)) {
					Node rhs = parseComparationExpr();
					if (rhs == null) {
						throw new ParserError(
								"Missing additive expression after '!='", this);
					}
					n = new Node(NodeType.NE, n, rhs, PREV_LINE_NUMBER());
				}
				break;
			}
		}
		return n;
	}

	// comparationExpr : additive (
	// '==' additive
	// )*
	public Node parseComparationExpr() throws ParserError {
		Node n = parseAdditive();
		if (n == null) {
			return null;
		}

		while (true) {
			if (EAT(TokenType.GE)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '>='", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.GE, children, PREV_LINE_NUMBER());
			} else if (EAT(TokenType.GT)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '>'", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.GT, children, PREV_LINE_NUMBER());
			} else if (EAT(TokenType.LE)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '<='", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.LE, children, PREV_LINE_NUMBER());
			} else if (EAT(TokenType.LT)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '<'", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.LT, children, PREV_LINE_NUMBER());
			} else {
				break;
			}
		}
		return n;
	}

	// additive: multitive ( '+' multitive )*
	// | multitive;
	public Node parseAdditive() throws ParserError {
		Node n = parseMultitive();

		while (true) {
			if (EAT(TokenType.PLUS)) {
				List<Node> children = new ArrayList<>();
				Node rhs = parseMultitive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '+'", this);
				}
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.ADD, children, PREV_LINE_NUMBER());
			} else if (EAT(TokenType.MINUS)) {
				List<Node> children = new ArrayList<>();
				Node rhs = parseMultitive();
				// TODO check rhs
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.SUBTRACT, children, PREV_LINE_NUMBER());
			} else if (EAT(TokenType.CONCAT)) {
				// '_' is the sring concatenation operator.
				List<Node> children = new ArrayList<>();
				Node rhs = parseMultitive();
				// TODO check rhs
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.CONCAT, children, PREV_LINE_NUMBER());
			} else {
				break;
			}
		}

		return n;
	}

	// multitive = atom '*' atom
	// | atom;
	private Node parseMultitive() throws ParserError {
		Node n = parseFuncall();

		while (true) {
			if (EAT(TokenType.MUL)) {
				n = MKBINOP(n, NodeType.MULTIPLY, this::parseMultitive);
			} else if (EAT(TokenType.DIVIDE)) {
				n = MKBINOP(n, NodeType.DIVIDE, this::parseMultitive);
			} else if (EAT(TokenType.MODULO)) {
				n = MKBINOP(n, NodeType.MODULO, this::parseMultitive);
			} else {
				break;
			}
		}

		return n;
	}

	private Node parseFuncall() throws ParserError {
		Node n = parseNot();
		if (EAT(TokenType.LPAREN)) {
			LinkedList<Node> args = parseArgs();
			args.addFirst(n);

			if (EAT(TokenType.RPAREN)) {
				n = new Node(NodeType.FUNCALL, args, PREV_LINE_NUMBER());
			} else {
				throw new ParserError("Missing closing paren after arguments",
						this);
			}
		}
		return n;
	}

	private LinkedList<Node> parseArgs() throws ParserError {
		LinkedList<Node> list = new LinkedList<>();

		if (CURRENT_TYPE() == TokenType.RPAREN) {
			return list;
		}

		while (pos < tokens.size()) {
			Node expr = parseExpr();
			if (expr == null) {
				throw new ParserError("Missing expression in arguments", this);
			}
			list.add(expr);

			if (EAT(TokenType.COMMA)) {
				continue;
			} else {
				return list;
			}
		}

		throw new ParserError("Missing closing paren after arguments", this);
	}

	private Node parseNot() throws ParserError {
		if (EAT(TokenType.NOT)) {
			Node n = parseAttr();
			if (n == null) {
				throw new ParserError("Missing expression after '!'", this);
			}
			return new Node(NodeType.NOT, n, PREV_LINE_NUMBER());
		} else {
			return parseAttr();
		}
	}

	private Node parseAttr() throws ParserError {
		Node n = parseAtom();
		if (n != null) {
			while (true) {
				if (EAT(TokenType.DOT)) {
					if (CURRENT_TYPE() == TokenType.IDENT) {
						Node rhs = parseIdent();
						if (rhs == null) {
							throw new ParserError("SHOULD NOT REACH HERE", this);
						}
						Node key = new Node(NodeType.STRING, rhs.getText(),
								PREV_LINE_NUMBER()); // Convert
						// to
						// STRING
						// from
						// IDENT.
						n = new Node(NodeType.ATTRIBUTE, n, key,
								PREV_LINE_NUMBER());
					} else if (CURRENT_TYPE() == TokenType.DOLLARVAR) {
						Node rhs = parseDollarVar();
						if (rhs == null) {
							throw new ParserError("SHOULD NOT REACH HERE", this);
						}
						n = new Node(NodeType.ATTRIBUTE, n, rhs,
								PREV_LINE_NUMBER());
					} else {
						throw new ParserError(
								"Missing (identifier|variable) after '.'.",
								this);
					}
				} else if (EAT(TokenType.LBRACKET)) { // ary[idx]
					Node key = parseExpr();
					if (key == null) {
						throw new ParserError("Missing expression after '['",
								this);
					}

					if (!EAT(TokenType.RBRACKET)) {
						throw new ParserError(
								"Missing closing bracket after '['.", this);
					}

					n = new Node(NodeType.ATTRIBUTE, n, key, PREV_LINE_NUMBER());
				} else {
					break;
				}
			}
		}
		return n;
	}

	private Node MKBINOP(Node lhs, NodeType type, ParserMethod method)
			throws ParserError {
		Node rhs = method.parse();
		if (rhs == null) {
			throw new ParserError("Missing " + method + ".", this);
		}
		return new Node(type, lhs, rhs, PREV_LINE_NUMBER());
	}

	// atom: double | int | ident | paren | ...;
	public Node parseAtom() throws ParserError {
		switch (CURRENT_TYPE()) {
		case DOUBLE:
			return parseDouble();
		case INTEGER:
			return parseInt();
		case LPAREN:
			return parseParen();
		case LBRACKET:
			return parseArray();
		case LBRACE:
			return parseMap();
		case STRING:
			return parseString();
		case FALSE:
			return parseFalse();
		case TRUE:
			return parseTrue();
		case NULL:
			return parseNull();
		case IDENT:
			return parseIdent();
		case DOLLARVAR:
			return parseDollarVar();
		case LOOP:
			return parseLoop();
		case FILE:
			return parseFile();
		case LINE:
			return parseLine();
		default:
			return null;
		}
	}

	private Node parseFile() {
		if (CURRENT_TYPE() == TokenType.FILE) {
			String fileName = CURRENT_FILENAME();
			if (fileName == null) {
				fileName = "-";
			}
			++pos;
			return new Node(NodeType.STRING, fileName, PREV_LINE_NUMBER());
		} else {
			return null;
		}
	}

	private Node parseLine() {
		if (CURRENT_TYPE() == TokenType.LINE) {
			int lineNumber = CURRENT_LINE_NUMBER();
			++pos;
			return new Node(NodeType.INTEGER, "" + lineNumber,
					PREV_LINE_NUMBER());
		} else {
			return null;
		}
	}

	private Node parseLoop() {
		if (EAT(TokenType.LOOP)) {
			Node node = null;
			try (PositionSaver p = new PositionSaver(this)) {
				if (EAT(TokenType.DOT)) {
					if (CURRENT_TYPE() == TokenType.IDENT) {
						if ("count".equals(CURRENT_STRING())) {
							++pos;
							p.commit();
							node = new Node(NodeType.LOOP_COUNT,
									PREV_LINE_NUMBER());
						} else if ("index".equals(CURRENT_STRING())) {
							++pos;
							p.commit();
							node = new Node(NodeType.LOOP_INDEX,
									PREV_LINE_NUMBER());
						} else if ("has_next".equals(CURRENT_STRING())) {
							++pos;
							p.commit();
							node = new Node(NodeType.LOOP_HAS_NEXT,
									PREV_LINE_NUMBER());
						}
					}
				}
			}
			if (node == null) {
				node = new Node(NodeType.LOOP_INDEX, PREV_LINE_NUMBER());
			}
			return node;
		} else {
			return null;
		}
	}

	private Node parseDollarVar() {
		if (CURRENT_TYPE() == TokenType.DOLLARVAR) {
			String name = CURRENT_STRING();
			++pos;
			return new Node(NodeType.DOLLARVAR, name, PREV_LINE_NUMBER());
		} else {
			return null;
		}
	}

	private Node parseNull() {
		++pos;
		return new Node(NodeType.NULL, PREV_LINE_NUMBER());
	}

	private Node parseTrue() {
		++pos;
		return new Node(NodeType.TRUE, PREV_LINE_NUMBER());
	}

	private Node parseFalse() {
		++pos;
		return new Node(NodeType.FALSE, PREV_LINE_NUMBER());
	}

	// map := '{' ( key '=>' value ',' )* '}'
	private Node parseMap() throws ParserError {
		if (!EAT(TokenType.LBRACE)) {
			return null;
		}

		List<Node> list = new ArrayList<>();

		while (pos < tokens.size()) {
			Node key = parseMapKey();
			if (key == null) {
				break;
			}

			if (!EAT(TokenType.ARROW)) {
				throw new ParserError("Missing => after map key", this);
			}

			Node value = parseExpr();
			if (value == null) {
				throw new ParserError("Missing expression after '=>'.", this);
			}

			list.add(key);
			list.add(value);

			if (!EAT(TokenType.COMMA)) {
				break;
			}
		}

		if (!EAT(TokenType.RBRACE)) {
			throw new ParserError("Missing '}' after map.", this);
		}

		return new Node(NodeType.MAP, list, PREV_LINE_NUMBER());
	}

	private Node parseMapKey() throws ParserError {
		Node retval = first(this::parseIdent, this::parseString);
		if (retval != null) {
			return retval;
		}
		return null;
	}

	@FunctionalInterface
	interface ParserMethod {
		Node parse() throws ParserError;
	}

	/**
	 * Utility method for parsing.
	 * 
	 * @return
	 * @throws ParserError
	 */
	public Node first(ParserMethod... methods) throws ParserError {
		for (ParserMethod method : methods) {
			Node ret = method.parse();
			if (ret != null) {
				return ret;
			}
		}
		return null;
	}

	private Node parseString() throws ParserError {
		if (CURRENT_TYPE() == TokenType.STRING) {
			Node node = new Node(NodeType.STRING, CURRENT_STRING(),
					PREV_LINE_NUMBER());
			++pos;
			return node;
		} else {
			return null;
		}
	}

	private Node parseArray() throws ParserError {
		if (EAT(TokenType.LBRACKET)) {
			List<Node> children = new ArrayList<>();
			while (true) {
				Node expr = parseExpr();
				if (expr == null) {
					break;
				}
				children.add(expr);

				if (!EAT(TokenType.COMMA)) {
					break;
				}
			}
			if (!EAT(TokenType.RBRACKET)) {
				throw this
						.createError("Missing closing bracket after array ltieral");
			}

			return new Node(NodeType.ARRAY, children, PREV_LINE_NUMBER());
		} else {
			return null;
		}
	}

	// paren: '(' expr ')';
	public Node parseParen() throws ParserError {
		if (EAT(TokenType.LPAREN)) {
			Node expr = parseExpr();
			if (expr != null) {
				if (EAT(TokenType.RPAREN)) {
					return expr;
				} else {
					throw this.createError("Closing paren is expected but : "
							+ CURRENT_TYPE());
				}
			} else {
				throw this.createError("No expression after '('");
			}
		} else {
			return null;
		}
	}

	private ParserError createError(String message) {
		return new ParserError(message, this);
	}

	public Node parseInt() {
		if (CURRENT_TYPE() == TokenType.INTEGER) {
			Node node = new Node(NodeType.INTEGER, CURRENT_STRING(),
					PREV_LINE_NUMBER());
			pos++;
			return node;
		} else {
			return null;
		}
	}

	public Node parseDouble() {
		if (CURRENT_TYPE() == TokenType.DOUBLE) {
			Node node = new Node(NodeType.DOUBLE, CURRENT_STRING(),
					PREV_LINE_NUMBER());
			pos++;
			return node;
		} else {
			return null;
		}
	}

	private boolean EXPECT_(TokenType type) {
		if (pos < tokens.size()) {
			return tokens.get(pos).getType() == type;
		} else {
			return false;
		}
	}

	/**
	 * Increment pos if current token is `type`.
	 * 
	 * @param type
	 * @return
	 */
	private boolean EAT(TokenType type) {
		if (EXPECT_(type)) {
			++pos;
			return true;
		} else {
			return false;
		}
	}

	private int CURRENT_LINE_NUMBER() {
		return tokens.get(pos).getLineNumber();
	}

	private String CURRENT_FILENAME() {
		if (pos < tokens.size()) {
			return tokens.get(pos).getFileName();
		} else {
			return null;
		}
	}

	private TokenType CURRENT_TYPE() {
		if (pos < tokens.size()) {
			return tokens.get(pos).getType();
		} else {
			return null;
		}
	}

	private boolean HAS_NEXT_TOKEN() {
		if (pos + 1 < tokens.size()) {
			return true;
		} else {
			return false;
		}
	}

	private TokenType NEXT_TYPE() {
		if (!HAS_NEXT_TOKEN()) {
			return null;
		}
		return tokens.get(pos + 1).getType();
	}

	private String CURRENT_STRING() {
		if (pos < tokens.size()) {
			return tokens.get(pos).getString();
		} else {
			return null;
		}
	}

	private int PREV_LINE_NUMBER() {
		if (pos == 0) {
			return tokens.get(0).getLineNumber();
		}
		return tokens.get(pos - 1).getLineNumber();
	}

	public Node parseIdent() {
		if (CURRENT_TYPE() == TokenType.IDENT) {
			Node node = new Node(NodeType.IDENT, CURRENT_STRING(),
					PREV_LINE_NUMBER());
			pos++;
			return node;
		} else {
			return null;
		}
	}

	void setPos(int pos) {
		this.pos = pos;
	}
}
