package me.geso.jtt.tt;

import com.google.common.collect.Lists;

import me.geso.jtt.Source;
import me.geso.jtt.lexer.LexerMode;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.lexer.TokenPair;
import me.geso.jtt.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TTLexer {
	private final Pattern closeTagRe;
	private final Pattern openTagRe;

	private static final Pattern intRe = Pattern
			.compile("\\A(?:[1-9][0-9]*|0)");
	private static final Pattern doubleRe = Pattern
			.compile("\\A(?:(?:[1-9][0-9]*|0)\\.[0-9]+)");

	private static final Pattern identRe = Pattern
			.compile("\\A(?:[a-zA-Z][_a-zA-Z0-9]*)");

	private static final Pattern positionRe = Pattern
			.compile("\\A__(FILE|LINE)__(?!\\w)");

	private int pos;
	private int lineNumber;
	private final String sourceString;

	private LexerMode mode;
	private List<Token> tokens;
	private final String closeTag;
	private final Source source;

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
			new TokenPair("AND", TokenType.LOOSE_AND), //
			new TokenPair("OR", TokenType.LOOSE_OR), //
			new TokenPair("WRAPPER", TokenType.WRAPPER), //
			new TokenPair("loop", TokenType.LOOP), //
			new TokenPair("true", TokenType.TRUE), //
			new TokenPair("false", TokenType.FALSE), //
			new TokenPair("null", TokenType.NULL) //
			);

	public TTLexer(Source source, String sourceString, String openTag, String closeTag) {
		this.source = source;
		this.sourceString = sourceString;
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
		this.lineNumber = 1;

		while (pos < sourceString.length()) {
			if (mode == LexerMode.IN_RAW) {
				this.lexRaw(sourceString);
			} else if (mode == LexerMode.IN_TAG) {
				this.lexTagBody(sourceString, tokens);
			} else {
				throw new RuntimeException("SHOULD NOT REACH HERE");
			}
		}

		if (mode == LexerMode.IN_TAG) {
			throw new TTLexerError("Missing closing tag", this);
		}

		return tokens;
	}

	public int getLine() {
		return this.lineNumber;
	}

	private void lexTagBody(String string, List<Token> tokens) {
		while (pos < string.length()) {
			// %]
			Matcher matcher = closeTagRe.matcher(string.substring(pos));
			if (matcher.find()) {
				pos += matcher.group(0).length();
				mode = LexerMode.IN_RAW;
				tokens.add(this.createToken(TokenType.CLOSE));
				return;
			} else {
				switch (string.charAt(pos)) {
				case '\n':
					++lineNumber;
					++pos;
					break;
				case ' ':
				case '\t':
					++pos;
					break;
				case '+':
					tokens.add(this.createToken(TokenType.PLUS));
					++pos;
					break;
				case '<':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							tokens.add(this.createToken(TokenType.LE));
							pos += 2;
						} else {
							tokens.add(this.createToken(TokenType.LT));
							pos++;
						}
					} else {
						tokens.add(this.createToken(TokenType.LT));
						++pos;
					}
					break;
				case '>':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							tokens.add(this.createToken(TokenType.GE));
							pos += 2;
						} else {
							tokens.add(this.createToken(TokenType.GT));
							pos++;
						}
					} else {
						tokens.add(this.createToken(TokenType.GT));
						++pos;
					}
					break;
				case '_':
					tokens.add(this.lexUnderScore());
					break;
				case '%':
					tokens.add(this.createToken(TokenType.MODULO));
					++pos;
					break;
				case '*':
					tokens.add(this.createToken(TokenType.MUL));
					++pos;
					break;
				case '-':
					tokens.add(this.createToken(TokenType.MINUS));
					++pos;
					break;
				case '/':
					tokens.add(this.createToken(TokenType.DIVIDE));
					++pos;
					break;
				case '!':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '=') {
							// !=
							tokens.add(this.createToken(TokenType.NE));
							++pos;
							++pos;
						} else {
							tokens.add(this.createToken(TokenType.NOT));
							++pos;
						}
					} else {
						tokens.add(this.createToken(TokenType.NOT));
						++pos;
					}
					break;
				case '|':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '|') {
							tokens.add(this.createToken(TokenType.OROR));
							++pos;
							++pos;
						} else {
							tokens.add(this.createToken(TokenType.PIPE));
							++pos;
						}
					} else {
						tokens.add(this.createToken(TokenType.PIPE));
						++pos;
					}
					break;
				case '&':
					if (pos + 1 < string.length()) {
						if (string.charAt(pos + 1) == '&') {
							tokens.add(this.createToken(TokenType.ANDAND));
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
							tokens.add(this.createToken(TokenType.RANGE));
							++pos;
							++pos;
						} else {
							tokens.add(this.createToken(TokenType.DOT));
							++pos;
						}
					} else {
						tokens.add(this.createToken(TokenType.DOT));
						++pos;
					}
					break;
				case '[':
					tokens.add(this.createToken(TokenType.LBRACKET));
					++pos;
					break;
				case ']':
					tokens.add(this.createToken(TokenType.RBRACKET));
					++pos;
					break;
				case '{':
					tokens.add(this.createToken(TokenType.LBRACE));
					++pos;
					break;
				case '?':
					tokens.add(this.createToken(TokenType.QUESTION));
					++pos;
					break;
				case ':':
					tokens.add(this.createToken(TokenType.KOLON));
					++pos;
					break;
				case '}':
					tokens.add(this.createToken(TokenType.RBRACE));
					++pos;
					break;
				case '(':
					tokens.add(this.createToken(TokenType.LPAREN));
					++pos;
					break;
				case ')':
					tokens.add(this.createToken(TokenType.RPAREN));
					++pos;
					break;
				case ',':
					tokens.add(this.createToken(TokenType.COMMA));
					++pos;
					break;
				case '=':
					if (pos + 1 < string.length()) {
						if (sourceString.substring(pos).startsWith("==")) {
							tokens.add(this.createToken(TokenType.EQUALS));
							pos += 2;
						} else if (sourceString.substring(pos).startsWith("=>")) {
							tokens.add(this.createToken(TokenType.ARROW));
							pos += 2;
						} else {
							tokens.add(this.createToken(TokenType.ASSIGN));
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
				case '$':
					++pos;
					tokens.add(lexDollarVar());
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

	private Token lexUnderScore() {
		Matcher matcher = positionRe.matcher(sourceString.substring(pos));
		if (matcher.find()) {
			pos += matcher.group(0).length();
			return this
					.createToken(matcher.group(1).equals("FILE") ? TokenType.FILE
							: TokenType.LINE);
		} else {
			++pos;
			return this.createToken(TokenType.CONCAT);
		}
	}

	private Token lexDollarVar() {
		Matcher matcher = identRe.matcher(sourceString.substring(pos));
		if (matcher.find()) {
			String name = matcher.group(0);
			pos += name.length();
			return this.createToken(TokenType.DOLLARVAR, name);
		} else {
			throw this.createError("Invalid token after '$'");
		}
	}

	private void lexLineComment() {
		while (pos < sourceString.length()) {
			if (sourceString.charAt(pos) == '\n') {
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
		while (pos < sourceString.length() && !finished) {
			switch (sourceString.charAt(pos)) {
			case '\\':
				if (pos + 1 < sourceString.length()) {
					switch (sourceString.charAt(pos + 1)) {
					default:
						++pos;
						builder.append(sourceString.charAt(pos));
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
				builder.append(sourceString.charAt(pos));
				++pos;
			}
		}
		return this.createToken(TokenType.STRING, new String(builder));
	}

	private Token lexDqString() {
		++pos;

		StringBuilder builder = new StringBuilder();
		boolean finished = false;
		while (pos < sourceString.length() && !finished) {
			switch (sourceString.charAt(pos)) {
			case '\\':
				if (pos + 1 < sourceString.length()) {
					switch (sourceString.charAt(pos + 1)) {
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
						builder.append(sourceString.charAt(pos));
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
				builder.append(sourceString.charAt(pos));
				++pos;
			}
		}
		return this.createToken(TokenType.STRING, new String(builder));
	}

	private Token lexOthers() {
		// keywords
		for (TokenPair keyword : keywords) {
			if (sourceString.substring(pos).startsWith(keyword.pattern)) {
				pos += keyword.pattern.length();
				return this.createToken(keyword.type);
			}
		}

		// ident.
		{
			Matcher matcher = identRe.matcher(this.sourceString.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return this.createToken(TokenType.IDENT, s);
			}
		}

		throw this.createError("Cannot tokenize template.");
	}

	private Token lexNumber(String string) {
		// double
		{
			Matcher matcher = doubleRe.matcher(this.sourceString.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return this.createToken(TokenType.DOUBLE, s);
			}
		}

		// lex int
		{
			Matcher matcher = intRe.matcher(this.sourceString.substring(pos));
			if (matcher.find()) {
				String s = matcher.group(0);
				pos += s.length();
				return this.createToken(TokenType.INTEGER, s);
			}
		}

		throw this.createError("Cannot tokenize number");
	}

	private TTLexerError createError(String message) {
		return new TTLexerError(message, this);
	}

	private void lexRaw(String string) {
		StringBuilder builder = new StringBuilder();
		while (pos < string.length()) {
			// [%-
			Matcher matcher = openTagRe.matcher(string.substring(pos));
			if (matcher.find()) {
				mode = LexerMode.IN_TAG;
				pos += matcher.group(0).length();

				if (builder.toString().length() > 0) {
					tokens.add(this.createToken(TokenType.RAW,
							builder.toString()));
				}

				// [%# comments %]
				if (pos < string.length() && string.charAt(pos) == '#') {
					this.lexTagComment();
				} else {
					tokens.add(this.createToken(TokenType.OPEN));
				}
				return;
			} else {
				char c = string.charAt(pos);
				if (c == '\n') {
					++lineNumber;
				}
				builder.append(string.charAt(pos));
				++pos;
			}
		}
		if (builder.toString().length() > 0) {
			tokens.add(this.createToken(TokenType.RAW, builder.toString()));
		}
	}

	private void lexTagComment() {
		Matcher commentMatcher = Pattern.compile(
				"\\A.*?" + Pattern.quote(closeTag), Pattern.DOTALL).matcher(
				sourceString.substring(pos));
		if (commentMatcher.find()) {
			this.pos += commentMatcher.group(0).length();
			this.mode = LexerMode.IN_RAW;
		} else {
			throw new TTLexerError("Missing closing tag after tag comments.",
					this);
		}

	}

	public int getPos() {
		return pos;
	}

	public String getSource() {
		return sourceString;
	}

	/**
	 * Shorthand for `createToken(type, null)`
	 * 
	 * @param type
	 * @return
	 */
	private Token createToken(TokenType type) {
		return this.createToken(type, null);
	}

	private Token createToken(TokenType type, String string) {
		return new Token(type, string, lineNumber, source.getFileName());
	}

}
