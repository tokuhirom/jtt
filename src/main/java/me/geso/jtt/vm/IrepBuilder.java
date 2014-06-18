package me.geso.jtt.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.parser.Node;

/**
 * This is a builder class for Irep.
 *
 * @author tokuhirom
 *
 */
public class IrepBuilder {
	private final List<Code> iseq = new ArrayList<Code>();
	private final List<Object> pool = new ArrayList<Object>();
	private final Map<Object, Integer> poolSeen = new HashMap<Object, Integer>();
	private final String fileName;
	private final List<Integer> lineNumbers = new ArrayList<>();

	public IrepBuilder(String fileName) {
		this.fileName = fileName;
	}

	public int getLineNumber(int pc) {
		return lineNumbers.get(pc);
	}

	public String getFileName() {
		return this.fileName;
	}

	public void addReturn() {
		iseq.add(new Code(OP.RETURN));
		if (lineNumbers.size() > 0) {
			lineNumbers.add(lineNumbers.get(lineNumbers.size()-1));
		} else {
			lineNumbers.add(1);
		}
	}

	public void add(OP op, Node node) {
		iseq.add(new Code(op));
		lineNumbers.add(node.getLineNumber());
	}

	public Code addLazy(OP op, Node node) {
		Code code = new Code(op);
		iseq.add(code);
		lineNumbers.add(node.getLineNumber());
		return code;
	}

	public void add(OP op, int i, Node node) {
		iseq.add(new Code(op, i));
		lineNumbers.add(node.getLineNumber());
	}

	/**
	 * Expand the object pool and put the index for pool as operand.
	 *
	 * @param op
	 * @param o
	 */
	public void addPool(OP op, Object o, Node node) {
		if (poolSeen.containsKey(o)) {
			Integer i = poolSeen.get(o);
			this.add(op, i, node);
		} else {
			pool.add(o);
			Integer i = pool.size() - 1;
			this.add(op, i, node);
			poolSeen.put(o, i);
		}
	}

	public int getSize() {
		return iseq.size();
	}

	public Irep build() {
		return new Irep(iseq.toArray(new Code[iseq.size()]),
				pool.toArray(new Object[pool.size()]));
	}

	public String toString() {
		return new Disassembler().disasm(build());
	}

}
