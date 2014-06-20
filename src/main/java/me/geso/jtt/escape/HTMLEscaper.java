package me.geso.jtt.escape;

import me.geso.jtt.JTTRawString;

import com.google.common.html.HtmlEscapers;

public class HTMLEscaper extends Escaper {
	@Override
	public String escape(String str) {
		if (str == null) {
			return "(null)";
		} else {
			return HtmlEscapers.htmlEscaper().escape(str);
		}
	}
	
	@Override
	public String escape(Object str) {
		if (str == null) {
			return "(null)";
		} else if (str instanceof JTTRawString) {
			return this.escape((JTTRawString)str);
		} else if (str instanceof Integer) {
			// Integer doesn't need HTML escape.
			return str.toString();
		} else {
			return HtmlEscapers.htmlEscaper().escape(str.toString());
		}
	}

	public String escape(JTTRawString str) {
		return str.toString();
	}

}
