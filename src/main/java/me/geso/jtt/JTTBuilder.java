package me.geso.jtt;

import java.nio.file.Path;
import java.util.List;

import me.geso.jtt.vm.VM;

public class JTTBuilder {
	private TemplateLoader.CacheLevel cacheLevel;
	private List<Path> includePaths;
	
	
	public JTTBuilder() {
	}

	public JTT build() {
		TemplateLoader loader = new TemplateLoader(getIncludePaths(), cacheLevel);
		Compiler compiler = new Compiler();
		VM vm = new VM(compiler, loader, null);
		return new JTT(vm, loader, compiler);
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
}