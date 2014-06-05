package me.geso.jtt.parser;

public interface Parser {
	public String getSource();
	public int getLine();
	public int getPos();
	public void setPos(int pos);
}
