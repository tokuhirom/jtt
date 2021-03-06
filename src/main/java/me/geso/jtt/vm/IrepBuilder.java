package me.geso.jtt.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Source;
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
	private final List<Integer> lineNumbers = new ArrayList<>();
	private final Source source;
	private int loopStackSize = 0;

	public IrepBuilder(Source source) {
		this.source = source;
	}

	public int getLineNumber(int pc) {
		return lineNumbers.get(pc);
	}

	public String getFileName() {
		return this.source.getFileName();
	}

	public void addReturn() {
		iseq.add(new Code(OP.RETURN));
		if (lineNumbers.size() > 0) {
			lineNumbers.add(lineNumbers.get(lineNumbers.size() - 1));
		} else {
			lineNumbers.add(1);
		}
	}

	public Code add(OP op, Node node) {
		Code code = new Code(op);
		iseq.add(code);
		lineNumbers.add(node.getLineNumber());
		return code;
	}

	public Code add(OP op, int a, Node node) {
		Code code = new Code(op, a);
		iseq.add(code);
		lineNumbers.add(node.getLineNumber());
		return code;
	}

	public Code add(OP op, int a, int b, Node node) {
		Code code = new Code(op, a, b);
		iseq.add(code);
		lineNumbers.add(node.getLineNumber());
		return code;
	}

	/**
	 * Expand the object pool and put the index for pool as operand.
	 *
	 * @param op
	 * @param o
	 * @return 
	 */
	public Code addPool(OP op, Object o, Node node) {
		if (poolSeen.containsKey(o)) {
			Integer i = poolSeen.get(o);
			return this.add(op, i, node);
		} else {
			pool.add(o);
			Integer i = pool.size() - 1;
			poolSeen.put(o, i);
			return this.add(op, i, node);
		}
	}

	public Code addPool(OP op, Object o, int dst, Node node) {
		if (poolSeen.containsKey(o)) {
			Integer i = poolSeen.get(o);
			return this.add(op, i, dst, node);
		} else {
			pool.add(o);
			Integer a = pool.size() - 1;
			poolSeen.put(o, a);
			return this.add(op, a, dst, node);
		}
	}

	public int getSize() {
		return iseq.size();
	}

	public Irep build(int registerNum) {
		return new Irep(iseq, pool, this.lineNumbers, this.source,
				this.loopStackSize, registerNum);
	}

	public void increaseLoopStackSize() {
		this.loopStackSize++;
	}

	public String toString() {
		return new Disassembler().disasm(build(0), -1);
	}

}
