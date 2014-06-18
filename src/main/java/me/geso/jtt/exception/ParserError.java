package me.geso.jtt.exception;

import java.io.IOException;
import me.geso.jtt.Source;
import me.geso.jtt.parser.Parser;

public class ParserError extends JTTError {
	private static final long serialVersionUID = 1L;
	private Parser parser;

	public ParserError(String msg, Parser parser) {
		super(msg);
		this.parser = parser;
	}

	public String toString() {
		int line = parser.getLine();
		Source src = parser.getSource();
		String lines;
		try {
			lines = src.getTargetLines(line);
		} catch (IOException e) {
			lines = "(IOException)";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(this.getMessage() + " at " + parser.getFileName() + " line "
				+ line);
		buf.append("\n==============================================\n");
		buf.append(lines);
		buf.append("\n==============================================");

		return buf.toString();
	}
}
