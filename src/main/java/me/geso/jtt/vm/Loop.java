package me.geso.jtt.vm;

import java.util.Iterator;

public class Loop {
	private int count;
	private Iterator<Object> iterator;
	private int pc;
	
	public Loop(Iterator<Object> iterator, int pc) {
		this.iterator = iterator;
		this.count = 0;
		this.pc = pc;
	}
	
	public int getCount() {
		return count;
	}
	
	public int getPC() {
		return pc;
	}
	
	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	public Object next() {
		++count;
		return iterator.next();
	}
}
