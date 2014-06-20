package me.geso.jtt.escape;

import com.google.common.html.HtmlEscapers;

public class HTMLEscaper implements Escaper {
	@Override
	public String escape(String str) {
		return HtmlEscapers.htmlEscaper().escape(str);
	}

}
