package me.geso.jtt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import me.geso.jtt.exception.JTTCompilerError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.VM;

public class JTT {
	private final VM vm;
	private final TemplateLoader loader;
	private final Compiler compiler;
	
	public JTT(VM vm, TemplateLoader loader, Compiler compiler) {
		this.vm = vm;
		this.loader = loader;
		this.compiler = compiler;
	}
	
	public String render(File file, Map<String,Object> vars) throws IOException, ParserError, JTTCompilerError, TemplateLoadingError {
		Irep irep = loader.compile(file.toPath(), compiler);
		String result = vm.run(irep, vars);
		return result;
	}

	public String renderString(String src, Map<String, Object> vars) throws ParserError, JTTCompilerError, TemplateLoadingError, IOException {
		if (vars == null) {
			vars = new HashMap<>();
		}
		Irep irep = compiler.compile("-", src);
		String result = vm.run(irep, vars);
		return result;
	}
}
