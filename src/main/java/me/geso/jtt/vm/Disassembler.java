package me.geso.jtt.vm;

public class Disassembler {
	public String disasm(Irep irep) {
		StringBuilder buffer = new StringBuilder();

		buffer.append(String.format("Iseq:\n"));

		Code[] iseq = irep.getIseq();
		for (int i = 0; i < iseq.length; ++i) {
			Code code = iseq[i];
			String detail = "";
			if (code.op == OP.LOAD_CONST) {
				Object obj = irep.getPool()[code.arg1];
				detail = obj == null ? "(null)" : " # PUSH(" + obj.toString() + ")";
			} else if (code.op == OP.LOAD_VAR) {
				Object obj = irep.getPool()[code.arg1];
				detail = obj == null ? "(null)" : " # " + obj.toString() + " = POP()";
			} else if (code.op == OP.SET_VAR) {
				Object obj = irep.getPool()[code.arg1];
				detail = obj == null ? "(null)" : " # " + obj.toString() + " = POP()";
			} else if (code.op == OP.JUMP) {
				detail = " # GOTO " + (code.arg1 + i);
			} else if (code.op == OP.JUMP_IF_FALSE) {
				detail = " # GOTO " + (code.arg1 + i);
			}
			buffer.append(String.format("  %06d %-15s %-4d %s\n", i, code.op, code.arg1, detail));
		}

		buffer.append(String.format("Pool:\n"));
		Object[] pool = irep.getPool();
		for (int i = 0; i < pool.length; ++i) {
			if (pool[i] == null) {
				buffer.append(String.format("  %6d null\n", i));
			} else {
				buffer.append(String.format("  %6d %s(%s)\n", i, pool[i].toString(),
						pool[i].getClass().toString()));
			}
		}

		return buffer.toString();
	}
}
