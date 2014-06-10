package me.geso.jtt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.exception.JTTCompilerError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.tt.TTSyntax;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.VM;

public class JTT {
	private final VM vm;
	private final TemplateLoader loader;
	private final Syntax syntax;
	
	public JTT(VM vm, TemplateLoader loader, Syntax syntax) {
		assert syntax != null;

		this.vm = vm;
		this.loader = loader;
		this.syntax = syntax;
	}
	
	public String render(File file, Map<String,Object> vars) throws IOException, ParserError, JTTCompilerError, TemplateLoadingError {
		Irep irep = loader.compile(file.toPath(), this.syntax);
		String result = vm.run(irep, vars);
		return result;
	}

	public String renderString(String src, Map<String, Object> vars) throws ParserError, JTTCompilerError, TemplateLoadingError, IOException {
		if (vars == null) {
			vars = new HashMap<>();
		}
		Syntax syntax = new TTSyntax("[%", "%]");
		List<Token> tokens = syntax.tokenize("-", src);
		Node ast = syntax.parse(src, tokens);
		Irep irep = syntax.compile(ast);
		String result = vm.run(irep, vars);
		return result;
	}
}
