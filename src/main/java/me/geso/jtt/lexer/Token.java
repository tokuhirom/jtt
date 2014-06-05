package me.geso.jtt.lexer;

public class Token {
	private TokenType type;
	private String string;

	public Token(TokenType type, String s) {
		this.type = type;
		this.string = s;
	}

	public Token(TokenType type) {
		this.type = type;
		this.string = null;
	}

	public TokenType getType() {
		return type;
	}

	public String getString() {
		return string;
	}

	@Override
	public String toString() {
		return "Token [type=" + type + ", string=" + string + "]";
	}

}
