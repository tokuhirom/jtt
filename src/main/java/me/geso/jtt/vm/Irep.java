package me.geso.jtt.vm;

import java.io.IOException;
import java.util.List;

import me.geso.jtt.Source;

public class Irep {
	private final Code[] iseq;
	private final Object[] pool;
	private final Integer[] lineNumbers;
	private final Source source;
	/**
	 * Save the last time string size.
	 */
	private int capacityHint = -1;
	private final int localVariableCount;

	public Irep(List<Code> iseq, List<Object> pool,
			List<Integer> lineNumbers, Source source, int localVariableCount) {
		this.iseq = iseq.toArray(new Code[iseq.size()]);
		this.pool = pool.toArray(new Object[pool.size()]);
		this.lineNumbers = lineNumbers.toArray(new Integer[lineNumbers.size()]);
		this.source = source;
		this.localVariableCount = localVariableCount;
	}

	public Code[] getIseq() {
		return iseq;
	}

	public Object[] getPool() {
		return pool;
	}

	public String toString() {
		return new Disassembler().disasm(this);
	}

	public int getLineNumber(int pos) {
		return lineNumbers[pos];
	}

	public String getFileName() {
		return source.getFileName();
	}
	
	public List<String> getSourceLines() throws IOException {
		return source.getSourceLines();
	}
	
	public int getCapacityHint() {
		return capacityHint;
	}

	public void setCapacityHint(int capacityHint) {
		this.capacityHint = capacityHint;
	}

	public int getLocalVariableCount() {
		return localVariableCount;
	}
}