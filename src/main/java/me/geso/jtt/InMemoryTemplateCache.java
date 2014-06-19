package me.geso.jtt;

import java.nio.file.Path;
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

	@Override
	public Irep get(Path filePath) {
		CacheEntry entry = cache.get(filePath.toAbsolutePath().toString());
		if (entry == null) {
			return null;
		} else {
			if (cacheMode == CacheMode.CACHE_WITH_UPDATE_CHECK) {
				if (filePath.toFile().lastModified() >= entry.mtime) {
					return null;
				}
			}
			return entry.irep;
		}
	}

	@Override
	public void set(Path filePath, Irep irep) {
		if (cacheMode == CacheMode.NO_CACHE) {
			return;
		}

		CacheEntry entry = new CacheEntry(irep, filePath.toFile()
				.lastModified());
		cache.put(filePath.toAbsolutePath().toString(), entry);
	}
	
	public int size() {
		return cache.size();
	}

}
