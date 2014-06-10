package me.geso.jtt;

import java.nio.file.Path;
import java.util.List;

public class TemplateLoadingError extends JTTError {
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return "TemplateLoadingError [includePaths=" + includePaths
				+ ", fileName=" + fileName + "]";
	}

	List<Path> includePaths;
	private Path fileName;

	public TemplateLoadingError(Path fileName, List<Path> includePaths) {
		this.fileName = fileName;
		this.includePaths = includePaths;
	}

}
