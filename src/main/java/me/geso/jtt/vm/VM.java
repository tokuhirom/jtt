package me.geso.jtt.vm;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.BaseStream;
import java.util.stream.IntStream;

import me.geso.jtt.Function;
import me.geso.jtt.JTTMessageListener;
import me.geso.jtt.JTTRawString;
import me.geso.jtt.Syntax;
import me.geso.jtt.TemplateLoader;
import me.geso.jtt.escape.Escaper;
import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.VMError;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.net.UrlEscapers;

/**
 * This is the Jslate Virtual Machine.
 *
 * @author tokuhirom
 */
public class VM {
	private final Escaper escaper;
	private final TemplateLoader loader;
	private final Syntax syntax;
	private final Map<String, Function> functions;
	// private boolean strictMode = false;
	private final JTTMessageListener warningListener;
	private final Irep irep;

	private static final Map<Class<?>, FieldAccess> fieldAccessCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, MethodAccess> methodAccessCache = new ConcurrentHashMap<>();

	/**
	 * VM innerr status.
	 */
	private final StringBuilder buffer;
	private final Object[] regs;
	private final Loop[] loopStack;
	private int pc;
	private int loopSP;
	private Map<String, Object> vars;

	private VM newVM(Irep irep, Map<String, Object> vars) {
		return new VM(syntax, loader, functions, warningListener, escaper,
				irep, vars);
	}

	public VM(Syntax syntax, TemplateLoader loader,
			Map<String, Function> functions,
			JTTMessageListener warningListener, Escaper escaper, Irep irep,
			Map<String, Object> vars) {
		if (vars == null) {
			throw new IllegalArgumentException("vars must not be null");
		}

		this.loopSP = 0;
		this.loader = loader;
		this.syntax = syntax;
		this.functions = functions;
		this.warningListener = warningListener;
		this.escaper = escaper;

		this.irep = irep;
		this.vars = vars;

		int capacityHint = irep.getCapacityHint();
		if (capacityHint >= 0) {
			this.buffer = new StringBuilder(capacityHint);
		} else {
			this.buffer = new StringBuilder();
		}
		this.regs = new Object[irep.getRegisterCount()];
		this.loopStack = new Loop[irep.getLoopStackSize()];

		this.pc = 0;
	}

	public String run() throws JTTError {

		Code[] codes = irep.getIseq();
		Object[] pool = irep.getPool();

		while (true) {
			Code code = codes[pc];

			// System.out.println(String.format("%06d %s", pc, code.op));

			switch (code.op) {
			case LOAD_CONST:
				assert code.b != -1;
				regs[code.b] = pool[code.a];
				++pc;
				break;
			case LOAD_INT:
				regs[code.b] = code.a;
				++pc;
				break;
			case APPEND_RAW: {
				buffer.append(pool[code.a]);
				++pc;
				break;
			}
			case APPEND:
				opAppend(code);
				break;
			case ADD: {
				regs[code.a] = doAdd(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case MODULO: {
				regs[code.a] = doModulo(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case SUBTRACT: {
				regs[code.a] = doSubtract(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case MULTIPLY: {
				regs[code.a] = doMultiply(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case DIVIDE: {
				regs[code.a] = doDivide(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case ANDAND: { // && operator
				regs[code.a] = convertToBoolean(regs[code.a])
						&& convertToBoolean(regs[code.b]);
				++pc;
				break;
			}
			case OROR: { // && operator
				regs[code.a] = convertToBoolean(regs[code.a])
						|| convertToBoolean(regs[code.b]);
				++pc;
				break;
			}
			case MATCH: // Smart match for SWITCH.
			case EQUALS: {
				regs[code.a] = doEquals(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case NE:
				regs[code.a] = doNotEquals(regs[code.a], regs[code.b]);
				++pc;
				break;
			case GT:
				regs[code.a] = doGT(regs[code.a], regs[code.b]);
				++pc;
				break;
			case GE:
				regs[code.a] = doGE(regs[code.a], regs[code.b]);
				++pc;
				break;
			case LT:
				regs[code.a] = doLT(regs[code.a], regs[code.b]);
				++pc;
				break;
			case LE:
				regs[code.a] = doLE(regs[code.a], regs[code.b]);
				++pc;
				break;
			case CONCAT:
				regs[code.a] = opConcat(regs[code.a], regs[code.b]);
				++pc;
				break;
			case LOAD_TRUE:
				regs[code.a] = Boolean.TRUE;
				++pc;
				break;
			case LOAD_FALSE:
				regs[code.a] = Boolean.FALSE;
				++pc;
				break;
			case LOAD_NULL:
				regs[code.a] = null;
				++pc;
				break;
			case MOVE:
				regs[code.a] = regs[code.b];
				++pc;
				break;
			case GET_ELEM: { // a[0], a['hoge']
				this.opGetElem(code);
				++pc;
				break;
			}
			case LOAD_VAR:
				opLoadVar(pool, code);
				break;
			case SET_VAR:
				opSetVar(pool, code);
				break;
			case MAKE_ARRAY: {
				LinkedList<Object> list = new LinkedList<Object>();
				for (int i = 0; i < code.b; ++i) {
					list.add(regs[code.a + i]);
				}
				regs[code.a] = list;
				++pc;
				break;
			}
			case RETURN: {
				String result = new String(buffer);
				this.irep.setCapacityHint(result.length());
				return result;
			}
			case FOR_START:
				opForStart(code);
				break;
			case FOR_ITER:
				opForIter(code);
				break;
			case JUMP:
				pc += code.a;
				break;
			case JUMP_ABS:
				pc = code.a;
				break;
			case JUMP_IF_FALSE: {
				boolean b = isTrue(regs[code.a]);
				if (!b) {
					pc += code.b;
				} else {
					++pc;
				}
				break;
			}
			case INCLUDE: {
				String path = (String) regs[code.a];
				Irep compiledIrep = loader.compile(Paths.get(path), syntax);
				String result = this.newVM(compiledIrep, vars).run();
				regs[code.a] = result;
				++pc;
				break;
			}
			case WRAP: {
				String path = (String) regs[code.a];
				Irep compiledIrep = loader.compile(Paths.get(path), syntax);
				HashMap<String, Object> newvars = new HashMap<>(vars);
				newvars.put("content", buffer.toString());
				buffer.delete(0, buffer.length());
				String result = this.newVM(compiledIrep, newvars).run();
				buffer.append(result);
				++pc;
				break;
			}
			case ATTRIBUTE: {
				regs[code.a] = this.getAttribute(regs[code.a], regs[code.b]);
				++pc;
				break;
			}
			case MAKE_MAP: {
				Map<String, Object> map = new HashMap<>();
				for (int i = 0; i < code.b; i += 2) {
					Object key = regs[code.a + i];
					Object value = regs[code.a + i + 1];
					map.put(key.toString(), value);
				}
				regs[code.a] = map;
				++pc;
				break;
			}
			case LC: {
				regs[code.a] = regs[code.a].toString().toLowerCase();
				++pc;
				break;
			}
			case UC: {
				regs[code.a] = regs[code.a].toString().toUpperCase();
				++pc;
				break;
			}
			case URI_ESCAPE: {
				regs[code.a] = UrlEscapers.urlFormParameterEscaper().escape(
						regs[code.a].toString());
				++pc;
				break;
			}
			case SPRINTF: {
				Object[] args = new Object[code.b - 1];
				for (int i = 1; i < code.b; ++i) {
					args[i - 1] = regs[code.a + i];
				}
				String format = regs[code.a].toString();
				String result = String.format(format, args);
				regs[code.a] = result;
				++pc;
				break;
			}
			case FUNCALL: {
				doFuncall(code);
				++pc;
				break;
			}
			case MAKE_RANGE: {
				doMakeRange(code);
				++pc;
				break;
			}
			case NOT: {
				regs[code.a] = !convertToBoolean(regs[code.a]);
				++pc;
				break;
			}
			case METHOD_CALL: {
				regs[code.a] = doMethodCall(code);
				++pc;
				break;
			}
			case LOOP_INDEX: {
				regs[code.a] = loopStack[loopSP - 1].getIndex();
				++pc;
				break;
			}
			case LOOP_COUNT: {
				regs[code.a] = loopStack[loopSP - 1].getCount();
				++pc;
				break;
			}
			case LOOP_HAS_NEXT:
				regs[code.a] = loopStack[loopSP - 1].hasNext();
				++pc;
				break;
			default:
				throw new RuntimeException("SHOULD NOT REACH HERE: " + code.op);
			}
		}
	}

	/**
	 * Append regs[A] into string builder.
	 * 
	 * @param code
	 */
	private void opAppend(Code code) {
		Object obj = regs[code.a];
		if (obj == null) {
			warn("Appending null");
			buffer.append("(null)");
		} else if (obj instanceof JTTRawString) {
			buffer.append(((JTTRawString) obj).toString());
		} else {
			buffer.append(escaper.escape(obj.toString()));
		}
		++pc;
	}

	private void opSetVar(Object[] pool, Code code) {
		vars.put((String) pool[code.a], regs[code.b]);
		++pc;
	}

	private void opLoadVar(Object[] pool, Code code) {
		regs[code.b] = vars.get(pool[code.a]);
		++pc;
	}

	/**
	 * Start new iterator. Created new iterator will put on the regs[A].
	 */
	private void opForStart(final Code code) {
		Object container = regs[code.a];
		Iterator<Object> iterator = this.getIterator(container);
		Loop loop = new Loop(iterator);
		loopStack[loopSP] = loop;
		++loopSP;

		++pc;
	}

	// Get iterator from top of stack.
	private Iterator<Object> getIterator(Object container) {
		if (container instanceof Collection) {
			// TODO better casting
			@SuppressWarnings("unchecked")
			Iterator<Object> iterator = ((Collection<Object>) container)
					.iterator();
			return iterator;
		} else if (container instanceof BaseStream) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Iterator<Object> iterator = ((BaseStream) container).iterator();
			return iterator;
		} else {
			throw this.createError("Non container type detected in FOREACH.");
		}
	}

	/**
	 * Get next element from the current iterator.
	 */
	private void opForIter(Code code) {
		// FOR_ITER()

		assert loopSP > 0;

		Loop loop = loopStack[loopSP - 1];
		if (loop.hasNext()) {
			regs[code.a] = loop.next();
			++pc;
		} else {
			// There is no rest element in iterator.
			--loopSP;
			pc = code.b;
		}
	}

	private Object doMethodCall(Code code) {
		Object[] params = new Object[code.b];
		for (int i = 0; i < code.b; ++i) {
			params[i] = regs[code.a + i + 2];
		}

		Object methodName = regs[code.a + 1];
		Object object = regs[code.a];

		try {
			MethodAccess access = methodAccessCache.get(object.getClass());
			if (access == null) {
				access = MethodAccess.get(object.getClass());
				methodAccessCache.put(object.getClass(), access);
			}

			Object retval = access
					.invoke(object, methodName.toString(), params);
			return retval;
		} catch (IllegalArgumentException e) {
			warn(e.toString());
			return null;
		}
	}

	private Boolean convertToBoolean(Object o) {
		if (o == null) {
			return false;
		} else if (o instanceof Boolean) {
			return (Boolean) o;
		} else if (o instanceof Integer) {
			return ((Integer) o).intValue() == 0;
		} else {
			return true;
		}
	}

	private void doMakeRange(Code code) throws JTTError {
		Object lhs = regs[code.a];
		Object rhs = regs[code.b];
		if (!(lhs instanceof Integer)) {
			throw new JTTError(
					"Left side of range construction operator should be Integer but : "
							+ lhs.getClass());
		}
		if (!(rhs instanceof Integer)) {
			throw new JTTError(
					"Right side of range construction operator should be Integer but : "
							+ rhs.getClass());
		}

		int startInclusive = ((Integer) lhs).intValue();
		int endInclusive = ((Integer) rhs).intValue();
		regs[code.a] = IntStream.rangeClosed(startInclusive, endInclusive);
	}

	private void doFuncall(Code code) {
		Object method = regs[code.a];
		if (method instanceof String) {
			Function function = this.functions.get(method);
			if (function != null) {
				Object[] objects = new Object[code.b];
				for (int i = 0; i < code.b; ++i) {
					objects[i] = regs[code.a + 1 + i];
				}
				regs[code.a] = function.call(objects);
			} else {
				warn("Unknown function: " + method);
				regs[code.a] = null;
			}
		} else {
			throw new RuntimeException("NIY: " + method.getClass());
		}
	}

	private Object getAttribute(Object container, Object index) {
		if (container instanceof Map) {
			if (index == null) {
				warn("attr is null");
				return null;
			}
			// TODO: remove SuppressWarnigns
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) container;
			Object value = map.get(index.toString());
			return value;
		} else if (container instanceof List) {
			if (index == null) {
				warn("null index for list index");
				return null;
			} else if (!(index instanceof Integer)) {
				warn("array index must be Integer but " + index.getClass());
				return null;
			}
			// TODO: remove SuppressWarnigns
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) container;
			return list.get((Integer) index);
		} else if (container == null) {
			warn("container is null");
			return null;
		} else {
			FieldAccess access;
			Class<? extends Object> klass = container.getClass();
			if (fieldAccessCache.containsKey(klass)) {
				access = fieldAccessCache.get(klass);
			} else {
				access = FieldAccess.get(container.getClass());
				fieldAccessCache.put(klass, access);
			}

			if (index instanceof String) {
				Object result = access.get(container, (String) index);
				return result;
			} else if (index == null) {
				warn("null index for accessor name");
				return null;
			} else {
				warn("Index is not a string: " + index.getClass());
				return null;
			}
		}
	}

	private Object opConcat(Object lhs, Object rhs) {
		StringBuilder builder = new StringBuilder();
		if (lhs == null) {
			warn("null in string concatenation.");
		} else {
			builder.append(lhs.toString());
		}
		if (rhs == null) {
			warn("null in string concatenation.");
		} else {
			builder.append(rhs.toString());
		}

		return builder.toString();
	}

	private void warn(String message) {
		// TODO We should show line number.
		if (warningListener != null) {
			warningListener.sendMessage(message, this.pc, this.irep);
		} else {
			System.out.println(message);
		}
	}

	private boolean isTrue(Object pop) {
		if (pop == null) {
			return false;
		}
		if (pop instanceof Boolean) {
			return ((Boolean) pop).booleanValue();
		}
		return true;
	}

	private void opGetElem(Code code) throws VMError {
		Object container = regs[code.a];
		Object key = regs[code.b];
		if (container instanceof Map) {
			Object elem = ((Map<?, ?>) container).get(key);
			regs[code.a] = elem;
		} else if (container instanceof List) {
			Object elem = ((List<?>) container).get((Integer) key);
			regs[code.a] = elem;
		} else {
			throw this.createError("Container must be List or Map: "
					+ container.getClass());
		}
	}

	private Object doAdd(Object lhs, Object rhs) throws VMError {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						+ ((Integer) rhs).intValue());
			} else {
				throw this.createError("rhs for '+' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						+ ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						+ ((Double) rhs).doubleValue());
			} else {
				throw this.createError("rhs for '+' must be Number");
			}
		} else {
			throw this.createError("lhs for '+' must be Number");
		}
	}

	private Object doSubtract(Object lhs, Object rhs) throws VMError {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						- ((Integer) rhs).intValue());
			} else {
				throw this.createError("rhs for '-' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						- ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						- ((Double) rhs).doubleValue());
			} else {
				throw this.createError("rhs for '-' must be Number");
			}
		} else {
			throw this.createError("lhs for '-' must be Number");
		}
	}

	private VMError createError(String message) {
		return new VMError(message, irep, pc);
	}

	private Object doMultiply(Object lhs, Object rhs) throws VMError {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						* ((Integer) rhs).intValue());
			} else {
				throw this.createError("rhs for '*' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						* ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						* ((Double) rhs).doubleValue());
			} else {
				throw this.createError("rhs for '*' must be Number");
			}
		} else {
			throw this.createError("lhs for '*' must be Number");
		}
	}

	private Object doDivide(Object lhs, Object rhs) throws VMError {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						/ ((Integer) rhs).intValue());
			} else {
				throw this.createError("rhs for '/' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						/ ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						/ ((Double) rhs).doubleValue());
			} else {
				throw this.createError("rhs for '/' must be Number");
			}
		} else {
			throw this.createError("lhs for '/' must be Number");
		}
	}

	private Object doModulo(Object lhs, Object rhs) throws VMError {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						% ((Integer) rhs).intValue());
			} else {
				throw this.createError("rhs for '+' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						% ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						% ((Double) rhs).doubleValue());
			} else {
				throw this.createError("rhs for '+' must be Number");
			}
		} else {
			throw this.createError("lhs for '+' must be Number");
		}
	}

	private Boolean doEquals(Object lhs, Object rhs) {
		if (lhs == null) {
			return rhs == null;
		}
		return new Boolean(lhs.equals(rhs));
	}

	private Object doNotEquals(Object lhs, Object rhs) {
		if (lhs == null) {
			return rhs != null;
		}
		return new Boolean(!lhs.equals(rhs));
	}

	// lhs > rhs
	private Object doGT(Object lhs, Object rhs) throws VMError {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret > 0;
		} else if (lhs == null) {
			throw this.createError("lhs is null for '>' operator");
		} else {
			throw this
					.createError("lhs for '>' must implement Comparable. But "
							+ lhs.getClass());
		}
	}

	// lhs >= rhs
	boolean doGE(Object lhs, Object rhs) throws VMError {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret >= 0;
		} else if (lhs == null) {
			throw this.createError("lhs is null for '>=' operator");
		} else {
			throw this
					.createError("lhs for '>' must implement Comparable. But "
							+ lhs.getClass());
		}
	}

	private Object doLT(Object lhs, Object rhs) throws VMError {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret < 0;
		} else if (lhs == null) {
			throw this.createError("lhs is null for '<' operator");
		} else {
			throw this
					.createError("lhs for '<' must implement Comparable. But "
							+ lhs.getClass());
		}
	}

	private Object doLE(Object lhs, Object rhs) throws VMError {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret <= 0;
		} else if (lhs == null) {
			throw this.createError("lhs is null for '<=' operator");
		} else {
			throw this
					.createError("lhs for '<=' must implement Comparable. But "
							+ lhs.getClass());
		}
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Regs: [");
		for (Object o : this.regs) {
			builder.append(o);
			builder.append(",");
		}
		builder.append("]\n");
		builder.append(new Disassembler().disasm(this.irep, this.pc));
		builder.append("Buffer:\n");
		builder.append(new String(this.buffer));
		return new String(builder);
	}

}
