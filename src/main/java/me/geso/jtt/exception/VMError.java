package me.geso.jtt.exception;

import java.io.IOException;
import java.util.List;

import me.geso.jtt.vm.Irep;

public class VMError extends JTTError {
	private final Irep irep;
	private final int pc;

	public VMError(String string, Irep irep, int pc) {
		super(string);
		this.irep = irep;
		this.pc = pc;
	}

	public String toString() {
		return this.getMessage() + "\n" + this.getCode();
	}

	private String getCode() {
		try {
			final int line = irep.getLineNumber(pc);
			final List<String> lines = irep.getSourceLines();
			final StringBuilder buf = new StringBuilder();
			buf.append("===================================================");
			for (int i = Math.max(0, line - 3); i < Math.min(lines.size(),
					line + 3); ++i) {
				buf.append(i == line - 1 ? "* " : "  ");
				buf.append(lines.get(i));
			}
			buf.append("===================================================");
			return buf.toString();
		} catch (IOException e) {
			return "";
		}
	}

	private static final long serialVersionUID = 1L;
}