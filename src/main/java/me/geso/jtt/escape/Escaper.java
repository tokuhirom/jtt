package me.geso.jtt.escape;

import me.geso.jtt.JTTRawString;

public abstract class Escaper {
	abstract public String escape(String string);
    public String escape(JTTRawString str) {
        return str.toString();
    }

	public String escape(Object str) {
		if (str == null) {
			return "(null)";
		} else if (str instanceof JTTRawString) {
			return this.escape((JTTRawString)str);
		} else {
			return this.escape(str.toString());
		}
	}
}
