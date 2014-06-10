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
	final TemplateCache templateCache;

	public TemplateLoader(List<Path> includePaths, TemplateCache templateCache) {
		this.includePaths = includePaths;
		this.templateCache = templateCache;
	}

	public Irep compile(Path fileName, Compiler compiler) throws JTTError {
		for (Path path : includePaths) {
			Path fullpath = path.resolve(fileName);
			if (fullpath.toFile().exists()) {
				{
					Irep irep = this.templateCache.get(fullpath);
					if (irep != null) {
						return irep;
					}
				}

				Irep irep = this.compileFile(fullpath, compiler);
				this.templateCache.set(fullpath, irep);
				return irep;
			}
		}
		throw new TemplateLoadingError(fileName, this.includePaths);
	}

	private Irep compileFile(Path fullpath, Compiler compiler) throws JTTError {
		try (InputStream is = Files.newInputStream(fullpath)) {
			String str = IOUtils.toString(is, "UTF-8");
			return compiler.compile(fullpath.toString(), str);
		} catch (IOException e) {
			throw new JTTError("Cannot load " + fullpath + " : "
					+ e.getMessage());
		}
	}
}
