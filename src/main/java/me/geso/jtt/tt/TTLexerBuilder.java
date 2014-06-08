package me.geso.jtt.tt;


public class TTLexerBuilder {
	private String openTag;
	private String closeTag;

	public TTLexerBuilder(String openTag, String closeTag) {
		this.openTag = openTag;
		this.closeTag = closeTag;
	}
	
	public TTLexer build(String src) {
		return new TTLexer(src, this.openTag, this.closeTag);
	}
}
