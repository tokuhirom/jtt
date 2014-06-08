package me.geso.jtt.tt;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import me.geso.jtt.lexer.Token;
import me.geso.jtt.lexer.TokenType;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.parser.Parser;
import me.geso.jtt.parser.ParserError;

import com.google.common.collect.Lists;

public class TTParser implements Parser {
	private int pos = 0;
	private String source;
	private int line = 1;
	private List<Token> tokens;

	public String getSource() {
		return this.source;
	}

	public int getPos() {
		return this.pos;
	}

	public TTParser(String source, List<Token> tokens) {
		this.source = source;
		this.tokens = tokens;
	}

	public Node parseTemplate() throws ParserError {
		return parseTemplateBody();
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
		return new Node(NodeType.TEMPLATE, children);
	}

	Node parseRawString() {
		if (CURRENT_TYPE() == TokenType.RAW) {
			Node node = new Node(NodeType.RAW_STRING, CURRENT_STRING());
			++pos;
			return node;
		} else {
			return null;
		}
	}

	// tag : '[%' expr '%]';
	public Node parseTag() throws ParserError {
		switch (tokens.get(pos).getType()) {
		case FOREACH: {
			++pos;

			Node iterNode = parseIdent();
			if (iterNode == null) {
				throw new ParserError("No variable name after (FOR|FOREACH)",
						this);
			}

			if (!EAT(TokenType.IN)) {
				throw new ParserError("No 'IN' keyword after (FOR|FOREACH)",
						this);
			}

			Node expr = parseExpr();
			if (expr == null) {
				throw new ParserError("No expression after (FOR|FOREACH)", this);
			}

			Node body = parseTemplateBody();
			if (body == null) {
				throw new ParserError("No template body after (FOR|FOREACH)",
						this);
			}

			if (!parseEnd()) {
				throw new ParserError("No `[% END %]` after (FOR|FOREACH)",
						this);
			}

			List<Node> children = new ArrayList<>();
			children.add(iterNode);
			children.add(expr);
			children.add(body);
			return new Node(NodeType.FOREACH, children);
		}
		case WHILE:
			return parseWhile();
		case LAST: {
			Node node = new Node(NodeType.LAST);
			++pos;
			return node;
		}
		case NEXT: {
			Node node = new Node(NodeType.NEXT);
			++pos;
			return node;
		}
		case IF:
			return parseIf();
		case INCLUDE:
			return parseInclude();
		case SET:
			return parseSet();
		case SWITCH:
			return parseSwitch();
		case END:
		case ELSE:
		case ELSIF:
		case CASE:
			// Unexpected [% END %] etc... Backtracking is required.
			return null;
		default: {
			Node exprNode = parseExpr();
			if (exprNode != null) {
				return new Node(NodeType.EXPRESSION, exprNode);
			} else {
				throw new ParserError("No expression", this);
			}
		}
		}
	}

	private Node parseSwitch() throws ParserError {
		if (!EAT(TokenType.SWITCH)) {
			return null;
		}

		List<Node> list = new ArrayList<Node>();

		Node expr = parseExpr();
		if (expr == null) {
			throw new ParserError("Missing expression after SWITCH", this);
		}
		list.add(expr);

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

		return new Node(NodeType.SWITCH, list);
	}

	// [% CASE expr %]
	private Node parseCase() throws ParserError {
		try (PositionSaver saver = new PositionSaver(this)) {
			if (!EAT(TokenType.CASE)) {
				return null;
			}

			// expr maybe null. `[% CASE %]` is valid.
			Node expr = parseExpr();

			Node body = parseTemplateBody();
			if (body == null) {
				throw new ParserError("Missing templates after CASE", this);
			}

			saver.commit();
			return new Node(NodeType.CASE, Lists.newArrayList(expr, body));
		}
	}

	// [% INCLUDE "hello.tt" %]
	// [% INCLUDE "hello.tt" WITH foo %]
	private Node parseInclude() throws ParserError {
		if (!EAT(TokenType.INCLUDE)) {
			throw new ParserError("No 'INCLUDE'", this);
		}

		Node path = parseString();

		if (EAT(TokenType.WITH)) {
			throw new ParserError("WITH is not implemented yet.", this);
		}

		return new Node(NodeType.INCLUDE, Lists.newArrayList(path));
	}

	private Node parseWhile() throws ParserError {
		if (!EAT(TokenType.WHILE)) {
			throw new ParserError("No 'WHILE'", this);
		}

		final Node expr = parseExpr();
		if (expr == null) {
			throw new ParserError("No expression after 'WHILE'", this);
		}

		final Node body = parseTemplateBody();

		if (!parseEnd()) {
			throw new ParserError("No END tag after 'IF'", this);
		}

		return new Node(NodeType.WHILE, Lists.newArrayList(expr, body));
	}

	private Node parseSet() throws ParserError {
		if (!EAT(TokenType.SET)) {
			throw new ParserError("No 'SET'", this);
		}

		final Node ident = parseIdent();
		if (!EAT(TokenType.ASSIGN)) {
			throw new ParserError("No = after 'SET'", this);
		}
		final Node expr = parseExpr();

		return new Node(NodeType.SET, Lists.newArrayList(ident, expr));
	}

	private Node parseIf() throws ParserError {
		// (if condition if-body else-body)
		if (!EAT(TokenType.IF)) {
			throw new ParserError("No 'IF'", this);
		}

		final Node cond = parseExpr();
		if (cond == null) {
			throw new ParserError("No condition after 'IF'", this);
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

		return new Node(NodeType.IF, children);
	}

	private Node parseElsIf() throws ParserError {
		if (!EAT(TokenType.ELSIF)) {
			return null;
		}

		final Node elsifCond = parseExpr();
		if (elsifCond == null) {
			throw new ParserError("No condition after 'ELSIF'", this);
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
		return new Node(NodeType.IF, list);
	}

	private Node parseElse() throws ParserError {
		if (!EAT(TokenType.ELSE)) {
			return null;
		}

		Node elseBody = parseTemplateBody();
		if (elseBody == null) {
			throw new ParserError("No body after 'ELSE'", this);
		}
		return elseBody;
	}

	class PositionSaver implements Closeable {
		Parser parser;
		int pos;
		boolean committed = false;

		public PositionSaver(Parser parser) {
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
		if (!EAT(TokenType.END)) {
			return false;
		} else {
			return true;
		}
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
		Node n = parseAssign();
		while (EAT(TokenType.PIPE)) {
			Node lhs = parseAssign();
			if (lhs == null) {
				throw new ParserError("Missing expression after '|'", this);
			}
			if (lhs.getType() == NodeType.IDENT) {
				ArrayList<Node> list = new ArrayList<>();
				list.add(n);
				n = new Node(NodeType.FUNCALL, lhs, n);
			} else {
				throw new ParserError("left side of pipe must be ident", this);
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
				n = new Node(NodeType.SET, children);
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
				n = new Node(NodeType.IF, children);
			}
		}
		return n;
	}

	public Node parseRangeExpr() throws ParserError {
		Node n = parseAndAnd();
		if (n != null) {
			if (EAT(TokenType.RANGE)) {
				Node l = parseAndAnd();
				if (l == null) {
					throw new ParserError("Missing expression after ':' : "
							+ CURRENT_TYPE(), this);
				}

				List<Node> children = new ArrayList<>();
				children.add(l);
				children.add(n);
				n = new Node(NodeType.RANGE, children);
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
					n = new Node(NodeType.ANDAND, n, rhs);
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
				if (EAT(TokenType.EQAULS)) {
					Node rhs = parseComparationExpr();
					if (rhs == null) {
						throw new ParserError(
								"Missing additive expression after '=='", this);
					}

					List<Node> children = new ArrayList<>();
					children.add(n);
					children.add(rhs);
					n = new Node(NodeType.EQAULS, children);
				} else if (EAT(TokenType.NE)) {
					Node rhs = parseComparationExpr();
					if (rhs == null) {
						throw new ParserError(
								"Missing additive expression after '!='", this);
					}
					n = new Node(NodeType.NE, n, rhs);
				}
				// TODO !=
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
				n = new Node(NodeType.GE, children);
			} else if (EAT(TokenType.GT)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '>'", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.GT, children);
			} else if (EAT(TokenType.LE)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '<='", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.LE, children);
			} else if (EAT(TokenType.LT)) {
				Node rhs = parseAdditive();
				if (rhs == null) {
					throw new ParserError(
							"Missing additive expression after '<'", this);
				}

				List<Node> children = new ArrayList<>();
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.LT, children);
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
				n = new Node(NodeType.ADD, children);
			} else if (EAT(TokenType.MINUS)) {
				List<Node> children = new ArrayList<>();
				Node rhs = parseMultitive();
				// TODO check rhs
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.SUBTRACT, children);
			} else if (EAT(TokenType.CONCAT)) {
				// '_' is the sring concatenation operator.
				List<Node> children = new ArrayList<>();
				Node rhs = parseMultitive();
				// TODO check rhs
				children.add(n);
				children.add(rhs);
				n = new Node(NodeType.CONCAT, children);
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
				n = new Node(NodeType.FUNCALL, args);
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
			return new Node(NodeType.NOT, n);
		} else {
			return parseAttr();
		}
	}

	private Node parseAttr() throws ParserError {
		Node n = parseAtom();
		while (true) {
			if (EAT(TokenType.DOT)) {
				Node rhs = parseIdent();
				if (rhs == null) {
					throw new ParserError("Missing identifier after '.'.", this);
				}
				n = new Node(NodeType.ATTRIBUTE, n, rhs);
			} else {
				break;
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
		return new Node(type, lhs, rhs);
	}

	// atom: double | int | ident | paren;
	// TODO Support double literal
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
		default:
			return null;
		}
	}

	private Node parseNull() {
		++pos;
		return new Node(NodeType.NULL);
	}

	private Node parseTrue() {
		++pos;
		return new Node(NodeType.TRUE);
	}

	private Node parseFalse() {
		++pos;
		return new Node(NodeType.FALSE);
	}

	// map := '{' ( key '=>' value ',' )* '}'
	private Node parseMap() throws ParserError {
		if (!EAT(TokenType.LBRACE)) {
			return null;
		}

		List<Node> list = new ArrayList<>();

		while (pos < source.length()) {
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

		return new Node(NodeType.MAP, list);
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
			Node node = new Node(NodeType.STRING, CURRENT_STRING());
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

			return new Node(NodeType.ARRAY, children);
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
			Node node = new Node(NodeType.INTEGER, CURRENT_STRING());
			pos++;
			return node;
		} else {
			return null;
		}
	}

	public Node parseDouble() {
		if (CURRENT_TYPE() == TokenType.DOUBLE) {
			Node node = new Node(NodeType.DOUBLE, CURRENT_STRING());
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

	private TokenType CURRENT_TYPE() {
		if (pos < tokens.size()) {
			return tokens.get(pos).getType();
		} else {
			return null;
		}
	}

	private String CURRENT_STRING() {
		return tokens.get(pos).getString();
	}

	public Node parseIdent() {
		if (CURRENT_TYPE() == TokenType.IDENT) {
			Node node = new Node(NodeType.IDENT, CURRENT_STRING());
			pos++;
			return node;
		} else {
			return null;
		}
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	@Override
	public void setPos(int pos) {
		this.pos = pos;
	}
}
