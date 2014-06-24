package me.geso.jtt;

import me.geso.jtt.vm.Irep;

public interface TemplateCache {
	public Irep get(String filePath);
	public void set(String filePath, Irep irep);
}
