package me.geso.jtt.tt;

import java.util.List;

import me.geso.jtt.exception.ParserError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.Compiler;
import me.geso.jtt.Syntax;

public class TTSyntax implements Syntax {
	private final String openTag;
	private final String closeTag;

	public TTSyntax(String openTag, String closeTag) {
		this.openTag = openTag;
		this.closeTag = closeTag;
	}

	public TTSyntax() {
		this.openTag = "[%";
		this.closeTag = "%]";
	}

	/* (non-Javadoc)
	 * @see me.geso.jtt.tt.Syntax#tokenize(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Token> tokenize(String fileName, String src) {
		TTLexer lexer = new TTLexer(fileName, src, openTag, closeTag);
		return lexer.lex();
	}
	
	/* (non-Javadoc)
	 * @see me.geso.jtt.tt.Syntax#parse(java.lang.String, java.util.List)
	 */
	@Override
	public Node parse(String source, List<Token> tokens) throws ParserError {
		return new TTParser(source, tokens).parseTemplate();
	}

	/* (non-Javadoc)
	 * @see me.geso.jtt.tt.Syntax#compile(me.geso.jtt.parser.Node)
	 */
	@Override
	public Irep compile(Node ast) throws ParserError {
		return new Compiler().compile(ast);
	}
}
