package me.geso.jtt;

import me.geso.jtt.vm.Irep;

/**
 * Null cache storage.
 *
 * @author tokuhirom
 *
 */
public class NullTemplateCache implements TemplateCache {

	@Override
	public Irep get(String filePath) {
		return null;
	}

	@Override
	public void set(String filePath, Irep irep) {
		// Do nothing.
	}

}
