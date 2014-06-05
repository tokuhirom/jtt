package me.geso.jtt.escape;

public class NullEscaper extends Escaper {

	@Override
	public String escape(String string) {
		return string;
	}

}
