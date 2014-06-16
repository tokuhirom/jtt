package me.geso.jtt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Function;
import me.geso.jtt.tt.TTSyntax;
import me.geso.jtt.vm.VM;

/**
 * This is a builder class for the JTT class.
 * 
 * @author tokuhirom
 *
 */
public class JTTBuilder {
	private List<Path> includePaths;
	private JTTMessageListener warningListener;
	private final Map<String, Function> functions = new HashMap<String,Function>();
	private TemplateCache templateCache = new NullTemplateCache();
	private Syntax syntax = new TTSyntax();
	
	public JTTBuilder() {
	}

	/**
	 * Build JTT class instance.
	 * 
	 * @return Created instance.
	 */
	public JTT build() {
		TemplateLoader loader = new TemplateLoader(getIncludePaths(), this.templateCache);
		VM vm = new VM(this.syntax, loader, functions, warningListener);
		JTT jtt = new JTT(vm, loader, this.syntax);
		return jtt;
	}

	public List<Path> getIncludePaths() {
		return includePaths;
	}

	public JTTBuilder addIncludePath(Path path) {
		if (this.includePaths == null) {
			this.includePaths = new ArrayList<>();
		}
		this.includePaths.add(path);
		return this;
	}

	public JTTBuilder setIncludePaths(List<Path> includePaths) {
		this.includePaths = includePaths;
		return this;
	}

	public JTTBuilder setWarningListener(JTTMessageListener warningListener) {
		this.warningListener = warningListener;
		return this;
	}
	
	public JTTBuilder addFunction(String name, Function function) {
		this.functions.put(name, function);
		return this;
	}

	public JTTBuilder setTemplateCache(TemplateCache templateCache) {
		this.templateCache = templateCache;
		return this;
	}

	public JTTBuilder setSyntax(Syntax syntax) {
		this.syntax = syntax;
		return this;
	}
}