package me.geso.jtt;

/**
 * This class indicates the string does not need to escape.
 * 
 * @author tokuhirom
 *
 */
public class JTTRawString {
	private String str;

	public JTTRawString(String s) {
		this.str = s;
	}
	
	public String toString() {
		return this.str;
	}
}
