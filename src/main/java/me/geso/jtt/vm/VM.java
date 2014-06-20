package me.geso.jtt.vm;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
	private final Stack<Object> stack;
	private final Stack<Loop> loopStack;
	private final ArrayList<Object> localVars;
	private int pc;
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
		this.stack = new Stack<Object>();
		this.loopStack = new Stack<Loop>();
		this.localVars = new ArrayList<Object>(irep.getLocalVariableCount());

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
				stack.push(pool[code.arg1]);
				++pc;
				break;
			case APPEND_RAW: {
				buffer.append(pool[code.arg1]);
				++pc;
				break;
			}
			case APPEND:
				opAppend();
				break;
			case ADD: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doAdd(lhs, rhs));
				++pc;
				break;
			}
			case MODULO: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doModulo(lhs, rhs));
				++pc;
				break;
			}
			case SUBTRACT: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doSubtract(lhs, rhs));
				++pc;
				break;
			}
			case MULTIPLY: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doMultiply(lhs, rhs));
				++pc;
				break;
			}
			case DIVIDE: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doDivide(lhs, rhs));
				++pc;
				break;
			}
			case ANDAND: { // && operator
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(convertToBoolean(lhs) && convertToBoolean(rhs));
				++pc;
				break;
			}
			case OROR: { // && operator
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(convertToBoolean(lhs) || convertToBoolean(rhs));
				++pc;
				break;
			}
			case MATCH: // Smart match for SWITCH.
			case EQUALS: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doEquals(lhs, rhs));
				++pc;
				break;
			}
			case NE: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doNotEquals(lhs, rhs));
				++pc;
				break;
			}
			case GT: {
				assert stack.size() >= 2;
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doGT(lhs, rhs));
				++pc;
				break;
			}
			case GE: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doGE(lhs, rhs));
				++pc;
				break;
			}
			case LT: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doLT(lhs, rhs));
				++pc;
				break;
			}
			case LE: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doLE(lhs, rhs));
				++pc;
				break;
			}
			case CONCAT:
				opConcat();
				++pc;
				break;
			case LOAD_TRUE:
				stack.push(Boolean.TRUE);
				++pc;
				break;
			case LOAD_FALSE:
				stack.push(Boolean.FALSE);
				++pc;
				break;
			case LOAD_NULL:
				stack.push(null);
				++pc;
				break;
			case SET_LVAR:
				opSetLvar(code);
				break;
			case LOAD_LVAR:
				opLoadLvar(code);
				break;
			case GET_ELEM: { // a[0], a['hoge']
				this.opGetElem();
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
				for (int i = 0; i < code.arg1; ++i) {
					list.addFirst(stack.pop());
				}
				stack.push(list);
				++pc;
				break;
			}
			case RETURN: {
				String result = new String(buffer);
				this.irep.setCapacityHint(result.length());
				return result;
			}
			case ITER_START:
				opIterStart();
				break;
			case FOR_ITER:
				opForIter();
				break;
			case JUMP:
				pc += code.arg1;
				break;
			case JUMP_ABS:
				pc = code.arg1;
				break;
			case JUMP_IF_FALSE: {
				boolean b = isTrue(stack.pop());
				if (!b) {
					pc += code.arg1;
				} else {
					++pc;
				}
				break;
			}
			case INCLUDE: {
				String path = (String) stack.pop();
				Irep compiledIrep = loader.compile(Paths.get(path), syntax);
				String result = this.newVM(compiledIrep, vars).run();
				stack.push(result);
				++pc;
				break;
			}
			case WRAP: {
				String path = (String) stack.pop();
				Irep compiledIrep = loader.compile(Paths.get(path), syntax);
				HashMap<String, Object> newvars = new HashMap<>(vars);
				newvars.put("content", buffer.toString());
				buffer.delete(0, buffer.length());
				String result = this.newVM(compiledIrep, newvars).run();
				buffer.append(result);
				++pc;
				break;
			}
			case ATTRIBUTE: { // PUSH(POP()[POP()])
				// container[index]
				Object index = stack.pop();
				Object container = stack.pop();
				stack.push(this.getAttribute(container, index));
				++pc;
				break;
			}
			case MAKE_MAP: {
				Map<String, Object> map = new HashMap<>();
				for (int i = 0; i < code.arg1; ++i) {
					Object value = stack.pop();
					Object key = stack.pop();
					map.put(key.toString(), value);
				}
				stack.push(map);
				++pc;
				break;
			}
			case LC: {
				Object s = stack.pop();
				stack.push(s.toString().toLowerCase());
				++pc;
				break;
			}
			case UC: {
				Object s = stack.pop();
				stack.push(s.toString().toUpperCase());
				++pc;
				break;
			}
			case URI_ESCAPE: {
				Object o = stack.pop();
				String escaped = UrlEscapers.urlFormParameterEscaper().escape(
						o.toString());
				stack.push(escaped);
				++pc;
				break;
			}
			case SPRINTF: {
				Object[] args = new Object[code.arg1 - 1];
				for (int i = 1; i < code.arg1; ++i) {
					args[code.arg1 - i - 1] = stack.pop();
				}
				String format = stack.pop().toString();
				String result = String.format(format, args);
				stack.push(result);
				++pc;
				break;
			}
			case FUNCALL: {
				doFuncall(code.arg1, stack);
				++pc;
				break;
			}
			case MAKE_RANGE: {
				doMakeRange(stack);
				++pc;
				break;
			}
			case NOT: {
				Boolean b = convertToBoolean(stack.pop());
				stack.push(!b);
				++pc;
				break;
			}
			case METHOD_CALL: {
				stack.push(doMethodCall(code.arg1, stack));
				++pc;
				break;
			}
			case LOOP: {
				stack.push(loopStack.lastElement());
				++pc;
				break;
			}
			default:
				throw new RuntimeException("SHOULD NOT REACH HERE: " + code.op);
			}
		}
	}

	private void opLoadLvar(Code code) {
		Object o = localVars.get(code.arg1);
		stack.push(o);
		++pc;
	}

	private void opSetLvar(Code code) {
		Object o = stack.pop();
		localVars.add(code.arg1, o);
		++pc;
	}

	private void opAppend() {
		Object obj = stack.pop();
		if (obj == null) {
			warn("Appending null");
			buffer.append("(null)");
		} else if (obj instanceof JTTRawString) {
			buffer.append(((JTTRawString)obj).toString());
		} else {
			buffer.append(escaper.escape(obj.toString()));
		}
		++pc;
	}

	private void opSetVar(Object[] pool, Code code) {
		// vars[pool[arg1]]=POP()
		Object tos = stack.pop();
		vars.put((String) pool[code.arg1], tos);
		++pc;
	}

	private void opLoadVar(Object[] pool, Code code) {
		Object v = vars.get(pool[code.arg1]);
		stack.push(v);
		++pc;
	}

	private void opIterStart() {
		// Get an iterator object from TOS.
		// Put iterator object on the loop stack.

		Iterator<Object> iterator = this.getIterator();
		Loop loop = new Loop(iterator, pc);
		loopStack.push(loop);
		stack.push(loop.next());

		++pc;
	}

	private void opForIter() {
		// FOR_ITER()

		assert loopStack.size() > 0;

		Loop loop = loopStack.lastElement();
		if (loop.hasNext()) {
			Object next = loop.next();
			stack.push(next);
			pc = loop.getPC() + 1;
		} else {
			loopStack.pop();
			++pc;
		}
	}

	private Object doMethodCall(int paramCount, Stack<Object> stack) {
		Object[] params = new Object[paramCount];
		for (int i = 0; i < paramCount; ++i) {
			params[paramCount - i - 1] = stack.pop();
		}

		Object methodName = stack.pop();
		Object object = stack.pop();

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

	private void doMakeRange(Stack<Object> stack) throws JTTError {
		Object lhs = stack.pop();
		Object rhs = stack.pop();
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
		stack.push(IntStream.rangeClosed(startInclusive, endInclusive));
	}

	private void doFuncall(int arglen, Stack<Object> stack) {
		Object method = stack.pop();
		if (method instanceof String) {
			Function function = this.functions.get(method);
			if (function != null) {
				Object[] objects = new Object[arglen];
				for (int i = 0; i < arglen; ++i) {
					objects[arglen - i - 1] = stack.pop();
				}
				stack.push(function.call(objects));
			} else {
				warn("Unknown function: " + method);
				for (int i = 0; i < arglen + 1; ++i) {
					stack.pop();
				}
				stack.push(null);
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

	private void opConcat() {
		final Object lhs = stack.pop();
		final Object rhs = stack.pop();

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

		stack.push(builder.toString());
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

	private void opGetElem() throws VMError {
		Object key = stack.pop();
		Object container = stack.pop();
		if (container instanceof Map) {
			Object elem = ((Map<?, ?>) container).get(key);
			stack.push(elem);
		} else if (container instanceof List) {
			Object elem = ((List<?>) container).get((Integer) key);
			stack.push(elem);
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

	// Get iterator from top of stack.
	private Iterator<Object> getIterator() {
		Object container = stack.pop();

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
}
