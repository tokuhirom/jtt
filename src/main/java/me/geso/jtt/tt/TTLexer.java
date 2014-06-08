package me.geso.jtt.tt;

import com.google.common.collect.Lists;
import me.geso.jtt.lexer.JSlateLexerError;
import me.geso.jtt.lexer.LexerMode;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTLexer {
	private final Pattern closeTagRe;
	private final Pattern openTagRe;

	private static final Pattern intRe = Pattern
			.compile("\\A(?:[1-9][0-9]*|0)");
	private static final Pattern doubleRe = Pattern
			.compile("\\A(?:[1-9][0-9]*\\.[0-9]+)");

	private static final Pattern identRe = Pattern
			.compile("\\A(?:[a-zA-Z][_a-zA-Z0-9]*)");

	private int pos;
	private int line;
	private final String source;

	private LexerMode mode;
	private List<Token> tokens;
	private final String closeTag;

	private static final List<TokenPair> keywords = Lists.newArrayList(
			new TokenPair("END", TokenType.END),//
			new TokenPair("SWITCH", TokenType.SWITCH),//
			new TokenPair("CASE", TokenType.CASE),//
			new TokenPair("WHILE", TokenType.WHILE),//
			new TokenPair("FOREACH", TokenType.FOREACH),//
			new TokenPair("FOR", TokenType.FOREACH),//
			new TokenPair("IF", TokenType.IF),//
			new TokenPair("ELSIF", TokenType.ELSIF),//
			new TokenPair("ELSE", TokenType.ELSE),//
			new TokenPair("INCLUDE", TokenType.INCLUDE), //
			new TokenPair("WITH", TokenType.WITH), //
			new TokenPair("IN", TokenType.IN), //
			new TokenPair("SET", TokenType.SET), //
			new TokenPair("LAST", TokenType.LAST), //
			new TokenPair("NEXT", TokenType.NEXT), //
			new TokenPair("AND", TokenType.AND), //
			new TokenPair("OR", TokenType.OR), //
			new TokenPair("loop", TokenType.LOOP), //
			new TokenPair("true", TokenType.TRUE), //
			new TokenPair("false", TokenType.FALSE), //
			new TokenPair("null", TokenType.NULL) //
			);

	public TTLexer(String src, String openTag, String closeTag) {
		this.source = src;
		this.closeTag = closeTag;
		this.openTagRe = Pattern.compile(String.format("\\A(?:\\n?[ ]*%s-|%s)",
				Pattern.quote(openTag), Pattern.quote(openTag)));
		this.closeTagRe = Pattern.compile(String.format("\\A(?:%s|-%s[ ]*\n?)",
				Pattern.quote(closeTag), Pattern.quote(closeTag)));
	}

	public List<Token> lex() {
		this.tokens = new ArrayList<Token>();
		this.mode = LexerMode.IN_RAW;
		this.pos = 0;
		this.line = 0;

		while (pos < source.length()) {
			if (mode == LexerMode.IN_RAW) {
				String raw = lexRaw(source);
				if (raw.length() > 0) {
					tokens.add(new Token(TokenType.RAW, raw));
				}
			} else if (mode == LexerMode.IN_TAG) {
				lexTagBody(source, tokens);
			} else {
				throw new RuntimeException("SHOULD NOT REACH HERE");
			}
		}

		if (mode == LexerMode.IN_TAG) {
			throw new JSlateLexerError("Missing closing tag", this);
		}

		return tokens;
	}

	public int getLine() {
		return this.line;
	}

	private void lexTagBody(String string, List<Token> tokens) {
		while (pos < string.length()) {
			// %]
			Matcher matcher = closeTagRe.matcher(string.substring(pos));
			if (matcher.find()) {
				pos += matcher.group(0).length();
				mode = LexerMode.IN_RAW;
				return;
			} else {
				switch (string.charAt(pos)) {
				case '\n':
					++line;
					++pos;
					break;
				case ' ':
				case '\t':
					++pos;
					break;
				case '+':
					tokens.add(new Token(TokenType.PLUS));
					++pos;
					break;
				case '<':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							tokens.add(new Token(TokenType.LE));
							pos += 2;
						} else {
							tokens.add(new Token(TokenType.LT));
							pos++;
						}
					} else {
						tokens.add(new Token(TokenType.LT));
						++pos;
					}
					break;
				case '>':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							tokens.add(new Token(TokenType.GE));
							pos += 2;
						} else {
							tokens.add(new Token(TokenType.GT));
							pos++;
						}
					} else {
						tokens.add(new Token(TokenType.GT));
						++pos;
					}
					break;
				case '_':
					tokens.add(new Token(TokenType.CONCAT));
					++pos;
					break;
				case '%':
					tokens.add(new Token(TokenType.MODULO));
					++pos;
					break;
				case '*':
					tokens.add(new Token(TokenType.MUL));
					++pos;
					break;
				case '-':
					tokens.add(new Token(TokenType.MINUS));
					++pos;
					break;
				case '/':
					tokens.add(new Token(TokenType.DIVIDE));
					++pos;
					break;
				case '!':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							// !=
							tokens.add(new Token(TokenType.NE));
							++pos;
							++pos;
						} else {
							tokens.add(new Token(TokenType.NOT));
							++pos;
						}
					} else {
						tokens.add(new Token(TokenType.NOT));
						++pos;
					}
					break;
				case '|':
					tokens.add(new Token(TokenType.PIPE));
					++pos;
					break;
				case '&':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '&') {
							tokens.add(new Token(TokenType.ANDAND));
							++pos;
							++pos;
						} else {
							throw this.createError("Invalid operator '&'");
						}
					} else {
						throw this.createError("Invalid operator '&'");
					}
					break;
				case '.':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '.') {
							tokens.add(new Token(TokenType.RANGE));
							++pos;
							++pos;
						} else {
							tokens.add(new Token(TokenType.DOT));
							++pos;
						}
					} else {
						tokens.add(new Token(TokenType.DOT));
						++pos;
					}
					break;
				case '[':
					tokens.add(new Token(TokenType.LBRACKET));
					++pos;
					break;
				case ']':
					tokens.add(new Token(TokenType.RBRACKET));
					++pos;
					break;
				case '{':
					tokens.add(new Token(TokenType.LBRACE));
					++pos;
					break;
				case '?':
					tokens.add(new Token(TokenType.QUESTION));
					++pos;
					break;
				case ':':
					tokens.add(new Token(TokenType.KOLON));
					++pos;
					break;
				case '}':
					tokens.add(new Token(TokenType.RBRACE));
					++pos;
					break;
				case '(':
					tokens.add(new Token(TokenType.LPAREN));
					++pos;
					break;
				case ')':
					tokens.add(new Token(TokenType.RPAREN));
					++pos;
					break;
				case ',':
					tokens.add(new Token(TokenType.COMMA));
					++pos;
					break;
				case '=':
					if (pos + 1 < string.length()) {
						if (source.substring(pos).startsWith("==")) {
							tokens.add(new Token(TokenType.EQAULS));
							pos += 2;
						} else if (source.substring(pos).startsWith("=>")) {
							tokens.add(new Token(TokenType.ARROW));
							pos += 2;
						} else {
							tokens.add(new Token(TokenType.ASSIGN));
							++pos;
						}
					} else {
						throw this.createError("Invalid token around '='");
					}
					break;
				case '"':
					tokens.add(this.lexDqString());
					break;
				case '\'':
					tokens.add(this.lexSqString());
					break;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					tokens.add(lexNumber(string));
					break;
				case '#': {
					// [% # comment %]
					lexLineComment();
					break;
				}
				default:
					tokens.add(lexOthers());
					break;
				}
			}
		}
	}

	private void lexLineComment() {
		while (pos < source.length()) {
			if (source.charAt(pos) == '\n') {
				return;
			}
			++pos;
		}
		return;
	}

	private Token lexSqString() {
		++pos;

		StringBuilder builder = new StringBuilder();
		boolean finished = false;
		while (pos < source.length() && !finished) {
			switch (source.charAt(pos)) {
			case '\\':
				if (pos + 1 < source.length()) {
					switch (source.charAt(pos + 1)) {
					default:
						++pos;
						builder.append(source.charAt(pos));
						++pos;
						break;
					}
				} else {
					throw this.createError("Cannot token source after '\\'");
				}
				break;
			case '\'':
				++pos;
				finished = true;
				break;
			default:
				builder.append(source.charAt(pos));
				++pos;
			}
		}
		return new Token(TokenType.STRING, new String(builder));
	}

	private Token lexDqString() {
		++pos;

		StringBuilder builder = new StringBuilder();
		boolean finished = false;
		while (pos < source.length() && !finished) {
			switch (source.charAt(pos)) {
			case '\\':
				if (pos + 1 < source.length()) {
					switch (source.charAt(pos + 1)) {
					case 't':
						builder.append("\t");
						pos += 2;
						break;
					case 'n':
						builder.append("\n");
						pos += 2;
						break;
					default:
						++pos;
						builder.append(source.charAt(pos));
						++pos;
						break;
					}
				} else {
					throw this.createError("Cannot token source after '\\'");
				}
				break;
			case '"':
				++pos;
				finished = true;
				break;
			default:
				builder.append(source.charAt(pos));
				++pos;
			}
		}
		return new Token(TokenType.STRING, new String(builder));
	}

	private Token lexOthers() {
		// keywords
		for (TokenPair keyword : keywords) {
			if (source.substring(pos).startsWith(keyword.pattern)) {
				pos += keyword.pattern.length();
				return new Token(keyword.type);
			}
		}

		// ident.
		{
			Matcher matcher = identRe.matcher(this.source.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return new Token(TokenType.IDENT, s);
			}
		}

		throw this.createError("Cannot tokenize template.");
	}

	private Token lexNumber(String string) {
		// double
		{
			Matcher matcher = doubleRe.matcher(this.source.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return new Token(TokenType.DOUBLE, s);
			}
		}

		// lex int
		{
			Matcher matcher = intRe.matcher(this.source.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return new Token(TokenType.INTEGER, s);
			}
		}

		throw this.createError("Cannot tokenize number");
	}

	private JSlateLexerError createError(String message) {
		return new JSlateLexerError(message, this);
	}

	private String lexRaw(String string) {
		StringBuilder builder = new StringBuilder();
		while (pos < string.length()) {
			// [%-
			Matcher matcher = openTagRe.matcher(string.substring(pos));
			if (matcher.find()) {
				mode = LexerMode.IN_TAG;
				pos += matcher.group(0).length();

				// [%# comments %]
				if (pos < string.length() && string.charAt(pos) == '#') {
					this.lexTagComment();
				}

				return builder.toString();
			} else {
				char c = string.charAt(pos);
				if (c == '\n') {
					++line;
				}
				builder.append(string.charAt(pos));
				++pos;
			}
		}
		return builder.toString();
	}

	private void lexTagComment() {
		Matcher commentMatcher = Pattern.compile(
				"\\A.*?" + Pattern.quote(closeTag), Pattern.DOTALL).matcher(
				source.substring(pos));
		if (commentMatcher.find()) {
			this.pos += commentMatcher.group(0).length();
			this.mode = LexerMode.IN_RAW;
		} else {
			throw new JSlateLexerError(
					"Missing closing tag after tag comments.", this);
		}

	}

	public int getPos() {
		return pos;
	}

	public String getSource() {
		return source;
	}
}
