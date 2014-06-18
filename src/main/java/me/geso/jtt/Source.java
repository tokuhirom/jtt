package me.geso.jtt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Template source.
 * 
 * @author tokuhirom
 *
 */
public class Source {
	private final SourceType type;
	private final String source;

	enum SourceType {
		FROM_FILE, FROM_STRING
	}

	public Source(SourceType type, String source) {
		this.type = type;
		this.source = source;
	}

	/**
	 * Create new source object from string.
	 * 
	 * @param source
	 * @return
	 */
	public static Source fromString(String source) {
		return new Source(SourceType.FROM_STRING, source);
	}

	/**
	 * Create new Souce object from file Name.
	 * 
	 * @param fileName
	 * @return
	 */
	public static Source fromFile(String fileName) {
		return new Source(SourceType.FROM_FILE, fileName);
	}

	public List<String> getSourceLines() throws IOException {
		if (this.type == SourceType.FROM_FILE) {
			return Files.readAllLines(new File(this.source).toPath());
		} else {
			List<String> list = new ArrayList<String>();
			String[] lines = source.split("\r?\n");
			for (String line : lines) {
				list.add(line);
			}
			return list;
		}
	}
	
	public String getTargetLines(int line) throws IOException {
		StringBuilder buf = new StringBuilder();
		List<String> lines = this.getSourceLines();
		for (int i=Math.max(0, line-3); i<Math.min(lines.size(), line+3); ++i) {
			buf.append(i==line-1 ? "* " : "  ");
			buf.append(lines.get(i) + "\n");
		}
		return new String(buf);
	}

	public String getFileName() {
		if (this.type == SourceType.FROM_FILE) {
			return this.source;
		} else {
			return null;
		}
	}
}
