package me.geso.jtt;

import java.nio.file.Path;

import me.geso.jtt.vm.Irep;

/**
 * Null cache storage.
 *
 * @author tokuhirom
 *
 */
public class NullTemplateCache implements TemplateCache {

	@Override
	public Irep get(Path filePath) {
		return null;
	}

	@Override
	public void set(Path filePath, Irep irep) {
		// Do nothing.
	}

}
