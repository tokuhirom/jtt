package me.geso.jtt.lexer;


public class TokenPair {
	public final String pattern;
	public final TokenType type;

	public TokenPair(String pattern, TokenType type) {
		this.pattern = pattern;
		this.type = type;
	}

	@Override
	public String toString() {
		return "TokenPair [pattern=" + pattern + ", type=" + type + "]";
	}

}