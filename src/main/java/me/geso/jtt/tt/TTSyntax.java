package me.geso.jtt.tt;

import java.util.List;

import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.ParserError;

public class TTSyntax {
	private final String openTag;
	private final String closeTag;

	public TTSyntax(String openTag, String closeTag) {
		this.openTag = openTag;
		this.closeTag = closeTag;
	}

	public List<Token> tokenize(String fileName, String src) {
		TTLexer lexer = new TTLexer(fileName, src, openTag, closeTag);
		return lexer.lex();
	}
	
	public Node parse(String source, List<Token> tokens) throws ParserError {
		return new TTParser(source, tokens).parseTemplate();
	}
}
