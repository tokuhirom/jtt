package me.geso.jtt.exception;

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
		String src = parser.getSource();
		String[] lines = src.split("\r?\n");
		
		StringBuilder buf = new StringBuilder();
		buf.append(this.getMessage() + " at " + parser.getFileName() + " line " + line);
		buf.append("\n==============================================\n");
		for (int i=Math.max(0, line-3); i<Math.min(lines.length-1, line+3); ++i) {
			buf.append(i==line-1 ? "* " : "  ");
			buf.append(lines[i] + "\n");
		}
		buf.append("\n==============================================");
		
		return buf.toString();
	}
}
