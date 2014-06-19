package me.geso.jtt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.vm.Irep;

public class TemplateLoader {
	final List<Path> includePaths;
	final TemplateCache templateCache;

	public TemplateLoader(List<Path> includePaths, TemplateCache templateCache) {
		this.includePaths = includePaths;
		this.templateCache = templateCache;
	}

	public Irep compile(Path fileName, Syntax syntax) throws JTTError {
		assert syntax != null;

		{
			Irep irep = this.templateCache.get(fileName);
			if (irep != null) {
				return irep;
			}
		}

		for (Path path : includePaths) {
			Path fullpath = path.resolve(fileName);
			if (fullpath.toFile().exists()) {
				Irep irep = this.compileFile(fullpath, syntax);
				this.templateCache.set(fileName, irep);
				return irep;
			}
		}
		throw new TemplateLoadingError(fileName, this.includePaths);
	}

	private Irep compileFile(Path fullpath, Syntax syntax) throws JTTError {
		try {
			byte[] bytes = Files.readAllBytes(fullpath);
			String src = new String(bytes, StandardCharsets.UTF_8);
			Source source = Source.fromFile(fullpath.toString());
			List<Token> tokens = syntax.tokenize(source, src);
			Node ast = syntax.parse(source, tokens);
			return syntax.compile(source, ast);
		} catch (IOException e) {
			throw new JTTError("Cannot load " + fullpath + " : "
					+ e.getMessage());
		}
	}
}
