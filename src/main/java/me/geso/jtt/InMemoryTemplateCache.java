package me.geso.jtt;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.geso.jtt.vm.Irep;

class CacheEntry {
	final Irep irep;
	final long mtime;

	CacheEntry(Irep irep, long mtime) {
		this.irep = irep;
		this.mtime = mtime;
	}
}

public class InMemoryTemplateCache implements TemplateCache {

	Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	public enum CacheMode {
		NO_CACHE, CACHE_WITH_UPDATE_CHECK, CACHE_BUT_DO_NOT_CHECK_UPDATES,
	}

	private final CacheMode cacheMode;

	public InMemoryTemplateCache(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	// TODO We should care the relative file path.
	@Override
	public Irep get(String filePath) {
		CacheEntry entry = cache.get(filePath);
		if (entry == null) {
			return null;
		} else {
			if (cacheMode == CacheMode.CACHE_WITH_UPDATE_CHECK) {
				if (new File(filePath).lastModified() >= entry.mtime) {
					return null;
				}
			}
			return entry.irep;
		}
	}

	@Override
	public void set(String filePath, Irep irep) {
		if (cacheMode == CacheMode.NO_CACHE) {
			return;
		}

		CacheEntry entry = new CacheEntry(irep,
				new File(filePath).lastModified());
		cache.put(filePath, entry);
	}

	public int size() {
		return cache.size();
	}

}
