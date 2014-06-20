package me.geso.jtt.escape;

public class NullEscaper implements Escaper {

	@Override
	public String escape(String string) {
		return string;
	}

}
