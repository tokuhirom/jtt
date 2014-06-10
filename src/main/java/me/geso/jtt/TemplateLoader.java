package me.geso.jtt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.vm.Irep;

import org.apache.commons.io.IOUtils;

public class TemplateLoader {
	final List<Path> includePaths;
	final CacheLevel cacheLevel;

	enum CacheLevel {
		NO_CACHE, CACHE_WITH_UPDATE_CHECK, CACHE_BUT_DO_NOT_CHECK_UPDATES
	}

	public TemplateLoader(List<Path> includePaths, CacheLevel cacheLevel) {
		this.includePaths = includePaths;
		this.cacheLevel = cacheLevel;
	}

	public Irep compile(Path fileName, Compiler compiler) throws JTTError {
		// TODO We should cache the compilation result.
		for (Path path : includePaths) {
			Path fullpath = path.resolve(fileName);
			if (fullpath.toFile().exists()) {
				return this.compileFile(fullpath, compiler);
			}
		}
		throw new TemplateLoadingError(fileName, this.includePaths);
	}

	private Irep compileFile(Path fullpath, Compiler compiler)
			throws JTTError {
		try (InputStream is = Files.newInputStream(fullpath)) {
			String str = IOUtils.toString(is, "UTF-8");
			return compiler.compile(fullpath.toString(), str);
		} catch (IOException e) {
			throw new JTTError("Cannot load "+ fullpath + " : " + e.getMessage());
		}
	}
}
