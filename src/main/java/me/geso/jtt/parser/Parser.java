package me.geso.jtt.parser;

import me.geso.jtt.Source;

public interface Parser {
	public Source getSource();
	public int getLine();
	public String getFileName();
}
