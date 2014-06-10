package me.geso.jtt;

import java.nio.file.Path;

import me.geso.jtt.vm.Irep;

public interface TemplateCache {
	public Irep get(Path filePath);
	public void set(Path filePath, Irep irep);
}
