package me.geso.jtt.tt;


public class TTLexerBuilder {
	// List<TokenPair> tagTokenTypes;
	// private Pattern regExp;
	private String openTag;
	private String closeTag;

	public TTLexerBuilder(String openTag, String closeTag) {
		this.openTag = openTag;
		this.closeTag = closeTag;

		/*
		tagTokenTypes = new ArrayList<>();

		add("+", TokenType.PLUS);
		add("-", TokenType.MINUS);
		add("/", TokenType.DIV);
		add("*", TokenType.MUL);
		add("_", TokenType.CONCAT);
		add("(", TokenType.LPAREN);
		add(")", TokenType.RPAREN);


		// literals
		addRegExp("[1-9][0-9]|0", TokenType.INTEGER);
		addRegExp("[a-zA-Z][a-zA-Z0-9_]*", TokenType.IDENT);
		addRegExp("[1-9][0-9]*\\.[0-9]+", TokenType.DOUBLE);

		regExp = this.buildRegExp();
		*/
	}
	
	public TTLexer build(String src) {
		return new TTLexer(src, this.openTag, this.closeTag);
	}
}
