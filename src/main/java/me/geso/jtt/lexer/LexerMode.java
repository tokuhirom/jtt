package me.geso.jtt.lexer;

/**
 * Lexer's internal mode.
 * 
 * <code>
	hogehgoe[% foo %]
	          ^^^^^ IN_TAG
	^^^^^^^^ IN_RAW
	</code>
 */
public enum LexerMode {
	IN_RAW, IN_TAG
}