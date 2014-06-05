package me.geso.jtt.escape;

import me.geso.jtt.JTTRawString;

import com.google.common.escape.CharEscaper;
import com.google.common.escape.Escapers;

public class HTMLEscaper extends Escaper {
	private static final CharEscaper HTML_CONTENT_ESCAPER = (CharEscaper) Escapers
			.builder().addEscape('"', "&quot;")
			// Note: "&apos;" is not defined in HTML 4.01.
			.addEscape('\'', "&#39;").addEscape('&', "&amp;")
			.addEscape('<', "&lt;").addEscape('>', "&gt;").build();

	@Override
	public String escape(String str) {
		if (str == null) {
			return "(null)";
		} else {
			return HTML_CONTENT_ESCAPER.escape(str);
		}
	}
	
	@Override
	public String escape(Object str) {
		if (str == null) {
			return "(null)";
		} else if (str instanceof JTTRawString) {
			return this.escape((JTTRawString)str);
		} else {
			return this.escape(str.toString());
		}
	}

	public String escape(JTTRawString str) {
		return str.toString();
	}

}
