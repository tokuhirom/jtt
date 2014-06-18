package me.geso.jtt;

import java.util.List;

import me.geso.jtt.exception.ParserError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.vm.Irep;

public interface Syntax {

	public abstract List<Token> tokenize(String fileName, String src);

	public abstract Node parse(String source, List<Token> tokens)
			throws ParserError;

	public abstract Irep compileFile(String fileName, Node ast) throws ParserError;
	public abstract Irep compileString(String source, Node ast) throws ParserError;

}