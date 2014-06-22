package me.geso.jtt.vm;

public class Code {
	/**
	 * The OP type.
	 */
	public final OP op;
	/**
	 * The A register
	 */
	public int a = -1;
	/**
	 * The B register.
	 */
	public int b = -1;

	public Code(OP op) {
		this.op = op;
	}

	public Code(OP op, int arg1) {
		this.op = op;
		this.a = arg1;
	}

	public Code(OP op, int arg1, int arg2) {
		this.op = op;
		this.a = arg1;
		this.b = arg2;
		
		if (this.op == OP.LOAD_CONST && this.b == -1) {
			throw new RuntimeException("Invalid constant");
		}
	}
	
	public String toString() {
		return "[" + this.op + " " + this.a + " " + this.b + "]";
	}
}