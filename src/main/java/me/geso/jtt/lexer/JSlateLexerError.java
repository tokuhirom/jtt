package me.geso.jtt.lexer;

import me.geso.jtt.JTTError;
import me.geso.jtt.tt.TTLexer;

public class JSlateLexerError extends JTTError {
	private static final long serialVersionUID = 1L;

	private final String message;
	private final TTLexer lexer;

	public JSlateLexerError(String message, TTLexer lexer) {
		super(message);
		this.message = message;
		this.lexer = lexer;
	}

	@Override
	public String toString() {
		int pos = lexer.getPos();
		String src = lexer.getSource();
		int show = Math.min(src.length() - pos, 10);

		return message + " : '" + src.substring(pos, pos + show)
				+ "'";
	}

}
