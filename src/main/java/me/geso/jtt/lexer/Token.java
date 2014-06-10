package me.geso.jtt.lexer;

public class Token {
	private TokenType type;
	private String string;
	private int lineNumber;
	private String fileName;

	public Token(TokenType type, String string, int lineNumber, String fileName) {
		this.type = type;
		this.string = string;
		this.lineNumber = lineNumber;
		this.fileName = fileName;
	}

	public Token(TokenType type, int lineNumber, String fileName) {
		this.type = type;
		this.string = null;
		this.lineNumber = lineNumber;
		this.fileName = fileName;
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

	public int getLineNumber() {
		return lineNumber;
	}

	public String getFileName() {
		return fileName;
	}
}
