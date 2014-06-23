package me.geso.jtt.vm;

import java.util.Iterator;

public class Loop {
	private int count;
	private Iterator<Object> iterator;
	
	public Loop(Iterator<Object> iterator) {
		this.iterator = iterator;
		this.count = 0;
	}
	
	public int getCount() {
		return count;
	}
	
	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	public Object next() {
		++count;
		return iterator.next();
	}

	public Object getIndex() {
		return count-1;
	}
}
