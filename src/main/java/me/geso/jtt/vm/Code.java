package me.geso.jtt.vm;

public class Code {
	public OP op;
	public int arg1 = 0;

	public Code(OP op) {
		this.op = op;
	}

	public Code(OP op, int arg1) {
		this.op = op;
		this.arg1 = arg1;
	}
	
	public String toString() {
		return "[" + this.op + " " + this.arg1 + "]";
	}
}