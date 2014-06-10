package me.geso.jtt;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Function;
import me.geso.jtt.vm.VM;

/**
 * This is a builder class for the JTT class.
 * 
 * @author tokuhirom
 *
 */
public class JTTBuilder {
	private TemplateLoader.CacheLevel cacheLevel;
	private List<Path> includePaths;
	private JTTMessageListener warningListener;
	private final Map<String, Function> functions = new HashMap<String,Function>();
	
	public JTTBuilder() {
	}

	/**
	 * Build JTT class instance.
	 * 
	 * @return Created instance.
	 */
	public JTT build() {
		TemplateLoader loader = new TemplateLoader(getIncludePaths(), cacheLevel);
		Compiler compiler = new Compiler();
		VM vm = new VM(compiler, loader, functions, warningListener);
		JTT jtt = new JTT(vm, loader, compiler);
		return jtt;
	}

	public TemplateLoader.CacheLevel getCacheLevel() {
		return cacheLevel;
	}

	public void setCacheLevel(TemplateLoader.CacheLevel cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	public List<Path> getIncludePaths() {
		return includePaths;
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
}