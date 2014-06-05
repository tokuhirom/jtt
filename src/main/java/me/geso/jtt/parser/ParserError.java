package me.geso.jtt.parser;

public class ParserError extends Exception {
	private static final long serialVersionUID = 1L;
	private Parser parser;

	public ParserError(String msg, Parser parser) {
		super(msg);
		this.parser = parser;
	}

	public String toString() {
		int pos = parser.getPos();
		String src = parser.getSource();
		int show = Math.min(src.length() - pos, 10);

		return this.getMessage() + " : '" + src.substring(pos, pos + show)
				+ "'";
	}
}
