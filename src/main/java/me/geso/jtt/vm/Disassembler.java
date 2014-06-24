package me.geso.jtt.vm;

/**
 * Disassembler for JTT's Irep.
 * 
 * @author tokuhirom
 *
 */
public class Disassembler {
	/**
	 * disassemble the irep.
	 * 
	 * @param irep
	 * @param currentPC
	 *            If you want to disassemble irep for the running vm, you can
	 *            pass the current PC for displaying current position. Pass -1
	 *            otherwise.
	 * @return
	 */
	public String disasm(Irep irep, int currentPC) {
		StringBuilder buffer = new StringBuilder();

		buffer.append(String.format("Iseq:\n"));

		Object[] pool = irep.getPool();
		Code[] iseq = irep.getIseq();
		for (int i = 0; i < iseq.length; ++i) {
			Code code = iseq[i];
			String detail = analyzeOP(irep, pool, code, i);
			buffer.append(String.format("  %s %06d %-15s %-4d,%-4d %s\n",
					i == currentPC ? "*" : " ", i, code.op, code.a, code.b,
					detail));
		}

		buffer.append(String.format("Pool:\n"));
		for (int i = 0; i < pool.length; ++i) {
			if (pool[i] == null) {
				buffer.append(String.format("  %6d null\n", i));
			} else {
				buffer.append(String.format("  %6d %s(%s)\n", i,
						escape(pool[i].toString()), escape(pool[i].getClass().toString())));
			}
		}

		return buffer.toString();
	}

	private String analyzeOP(Irep irep, Object[] pool, Code code, int pc) {
		switch (code.op) {
		case LOAD_CONST: {
			Object obj = irep.getPool()[code.a];
			return obj == null ? "(null)" : String.format(" # regs[%d] = %s",
					code.b, obj.toString());
		}
		case ADD: {
			return String.format(" # regs[%d] = regs[%d] + regs[%d]", code.a,
					code.a, code.b);
		}
		case LOAD_INT:
			return String.format(" # regs[%d] = %d", code.b, code.a);
		case MOVE:
			return String.format(" # regs[%d] = regs[%d]", code.a, code.b);
		case MAKE_ARRAY:
			return String.format(" # regs[%d] = regs[%d]..regs[%d]", code.a,
					code.a, code.a + code.b);
		case GET_ELEM:
			return String.format(" # regs[%d] = getElem(regs[%d], regs[%d])",
					code.a, code.a, code.b);
		case APPEND:
			return String.format(" # append(regs[%d])", code.a);
		case LOAD_VAR:
			return String
					.format(" # regs[%d] = vars[regs[%d]]", code.b, code.a);
		case SET_VAR:
			return String
					.format(" # vars[%s] = regs[%d]", pool[code.a], code.b);
		case GT:
			return String.format(" # regs[%s] = regs[%d] > regs[%d]", code.a,
					code.a, code.b);
		case GE:
			return String.format(" # regs[%s] = regs[%d] >= regs[%d]", code.a,
					code.a, code.b);
		case LT:
			return String.format(" # regs[%s] = regs[%d] < regs[%d]", code.a,
					code.a, code.b);
		case LE:
			return String.format(" # regs[%s] = regs[%d] <= regs[%d]", code.a,
					code.a, code.b);
		case FOR_START:
			return String.format(" # MAKE_ITER(regs[%d])", code.a);
		case FOR_ITER:
			return String.format(" # regs[%d] = FOR_ITER(); GOTO %d IF END", code.a, code.b);
		case MODULO:
			return String.format(" # regs[%d] = regs[%d] %% regs[%d]", code.a,
					code.a, code.b);
		case MAKE_RANGE:
			return String.format(" # regs[%d] = regs[%d]..regs[%d]", code.a,
					code.a, code.b);
		case SUBTRACT:
			return String.format(" # regs[%d] = regs[%d] - regs[%d]", code.a,
					code.a, code.b);
		case FUNCALL:
			return String.format(" # regs[%d] = funcall(regs[%d]..regs[%d])",
					code.a, code.a, code.b);
		case APPEND_RAW: {
			Object obj = irep.getPool()[code.a];
			return obj == null ? "(null)" : " # APPEND_RAW("
					+ escape(obj.toString()) + ")";
		}
		case JUMP:
			return " # GOTO " + (code.a + pc);
		case JUMP_IF_FALSE:
			return " # GOTO " + (code.b + pc) + " UNLESS " + code.a;
		default: // Support all types.
			return "";
		}
	}

	private String escape(String s) {
		s = s.replace("\\", "\\\\");
		s = s.replace("\n", "\\n");
		return s;
	}
}
