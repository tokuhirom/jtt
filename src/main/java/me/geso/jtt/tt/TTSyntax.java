package me.geso.jtt.tt;

import java.util.List;

import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.ParserError;

public class TTSyntax {
	private TTLexerBuilder lexerBuilder;

	public TTSyntax(String openTag, String closeTag) {
		this.lexerBuilder = new TTLexerBuilder(openTag, closeTag);
	}

	public List<Token> tokenize(String fileName, String src) {
		TTLexer lexer = lexerBuilder.build(fileName, src);
		return lexer.lex();
	}
	
	public Node parse(String source, List<Token> tokens) throws ParserError {
		return new TTParser(source, tokens).parseTemplate();
	}
}
