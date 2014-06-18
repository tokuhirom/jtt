package me.geso.jtt;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.exception.JTTError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.tt.TTSyntax;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.VM;

public class JTT {
	private final TemplateLoader loader;
	private final Syntax syntax;
	private Map<String, Function> functions;
	private JTTMessageListener warningListener;

	public JTT(TemplateLoader loader, Syntax syntax,
			Map<String, Function> functions, JTTMessageListener warningListener) {
		if (syntax == null) {
			throw new IllegalArgumentException("syntax");
		}
		this.loader = loader;
		this.syntax = syntax;
		this.functions = functions;
		this.warningListener = warningListener;
	}

	public String render(String file, Map<String, Object> vars) throws JTTError {
		return this.render(new File(file), vars);
	}

	public String render(File file, Map<String, Object> vars) throws JTTError {
		Irep irep = loader.compile(file.toPath(), this.syntax);
		String result = this.newVM(irep, vars).run();
		return result;
	}

	public String renderString(String src, Map<String, Object> vars)
			throws JTTError {
		if (vars == null) {
			vars = new HashMap<>();
		}
		Syntax syntax = new TTSyntax("[%", "%]");
		List<Token> tokens = syntax.tokenize("-", src);
		Node ast = syntax.parse(src, tokens);
		Irep irep = syntax.compileString(src, ast);
		String result = this.newVM(irep, vars).run();
		return result;
	}

	private VM newVM(Irep irep, Map<String, Object> vars) {
		return new VM(syntax, loader, functions, warningListener, irep, vars);
	}
}
