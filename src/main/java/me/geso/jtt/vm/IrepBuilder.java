package me.geso.jtt.vm;

import java.util.ArrayList;
import java.util.List;

/**
* This is a builder class for Irep.
*
* @author tokuhirom
*
*/
public class IrepBuilder {
	private List<Code> iseq = new ArrayList<Code>();
	private List<Object> pool = new ArrayList<Object>();

	public void add(OP op) {
		iseq.add(new Code(op));
	}

	public Code addLazy(OP op) {
		Code code = new Code(op);
		iseq.add(code);
		return code;
	}
	
	public void add(OP op, int i) {
		iseq.add(new Code(op, i));
	}

	/**
	* Expand the object pool and put the index for pool as operand.
	*
	* @param op
	* @param o
	*/
	public void addPool(OP op, Object o) {
		pool.add(o);
		this.add(op, pool.size() - 1);
	}

	public int getSize() {
		return iseq.size();
	}

	public Irep build() {
		return new Irep(
				iseq.toArray(new Code[iseq.size()]),
				pool.toArray(new Object[pool.size()]));
	}
	
	public String toString() {
		return new Disassembler().disasm(build());
	}

}
