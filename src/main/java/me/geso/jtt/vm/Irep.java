package me.geso.jtt.vm;

import java.util.List;

public class Irep {
	private Code[] iseq;
	private Object[] pool;

	public Irep(List<Code> iseq, List<Object> pool) {
		this.iseq = iseq.toArray(new Code[iseq.size()]);
		this.pool = pool.toArray(new Object[iseq.size()]);
	}

	public Irep(Code[] iseq, Object[] pool) {
		this.iseq = iseq;
		this.pool = pool;
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
}