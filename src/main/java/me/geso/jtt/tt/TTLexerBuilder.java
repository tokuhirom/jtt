package me.geso.jtt.tt;


public class TTLexerBuilder {
	private String openTag;
	private String closeTag;

	public TTLexerBuilder(String openTag, String closeTag) {
		this.openTag = openTag;
		this.closeTag = closeTag;
	}
	
	public TTLexer build(String fileName, String src) {
		return new TTLexer(fileName, src, this.openTag, this.closeTag);
	}
}
