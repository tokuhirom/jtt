package me.geso.jtt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.escape.Escaper;
import me.geso.jtt.escape.HTMLEscaper;
import me.geso.jtt.tt.TTSyntax;

/**
 * This is a builder class for the JTT class.
 * 
 * @author tokuhirom
 *
 */
public class JTTBuilder {
	private List<Path> includePaths = new ArrayList<>();
	private JTTMessageListener warningListener;
	private final Map<String, Function> functions = new HashMap<String,Function>();
	private TemplateCache templateCache = new NullTemplateCache();
	private Syntax syntax = new TTSyntax();
	private Escaper escaper = new HTMLEscaper();
	
	public JTTBuilder() {
	}

	/**
	 * Build JTT class instance.
	 * 
	 * @return Created instance.
	 */
	public JTT build() {
		TemplateLoader loader = new TemplateLoader(getIncludePaths(), this.templateCache);
		JTT jtt = new JTT(loader, this.syntax, functions, warningListener, escaper);
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
	
	public JTTBuilder setEscaper(Escaper escaper) {
		this.escaper = escaper;
		return this;
	}

	public JTTBuilder setSyntax(Syntax syntax) {
		if (syntax == null) {
			throw new Error("Syntax must not be null");
		}
		this.syntax = syntax;
		return this;
	}
}