package me.geso.jtt.vm;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import me.geso.jtt.Compiler;
import me.geso.jtt.TemplateLoader;
import me.geso.jtt.TemplateLoadingError;
import me.geso.jtt.escape.Escaper;
import me.geso.jtt.escape.HTMLEscaper;
import me.geso.jtt.parser.ParserError;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.net.UrlEscapers;

/**
 * This is the Jslate Virtual Machine.
 *
 * @author tokuhirom
 */
public class VM {
	private Escaper escaper = new HTMLEscaper();
	private TemplateLoader loader;
	private Compiler compiler;
	private final Map<String, Function> functions;
	private boolean strictMode = false;

	public VM(Compiler compiler, TemplateLoader loader,
			Map<String, Function> functions) {
		this.loader = loader;
		this.compiler = compiler;
		this.functions = functions;
	}

	public String run(Irep irep, Map<String, Object> vars)
			throws JSlateException, IOException, ParserError,
			TemplateLoadingError {
		int pc = 0;

		Code[] codes = irep.getIseq();
		Object[] pool = irep.getPool();
		StringBuilder buffer = new StringBuilder();
		Stack<Object> stack = new Stack<Object>();
		Stack<Loop> loopStack = new Stack<Loop>();
		Stack<ArrayList<Object>> localVarStack = new Stack<ArrayList<Object>>();
		localVarStack.add(new ArrayList<Object>());

		while (true) {
			Code code = codes[pc];

			// System.out.println(String.format("%06d %s", pc, code.op));
			// TODO INCLUDE

			switch (code.op) {
			case LOAD_CONST:
				stack.push(pool[code.arg1]);
				++pc;
				break;
			case APPEND: {
				Object obj = stack.pop();
				if (obj == null) {
					warn("Appending null");
				}
				buffer.append(escaper.escape(obj));
				++pc;
				break;
			}
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
			case EQAULS: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doEqauls(lhs, rhs));
				++pc;
				break;
			}
			case NE: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doNotEqauls(lhs, rhs));
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
			case CONCAT: {
				Object lhs = stack.pop();
				Object rhs = stack.pop();
				stack.push(doConcat(lhs, rhs));
				++pc;
				break;
			}
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
			case SET_LVAR: {
				Object o = stack.pop();
				localVarStack.lastElement().add(code.arg1, o);
				++pc;
				break;
			}
			case LOAD_LVAR: {
				Object o = localVarStack.lastElement().get(code.arg1);
				stack.push(o);
				++pc;
				break;
			}
			case INVOKE: { // invoke method
				Object methodName = stack.pop();
				Object self = stack.pop();
				stack.push(this.invokeMethod(methodName, self, code.arg1));
				++pc;
				break;
			}
			case ELEM: { // a[0], a['hoge']
				Object key = stack.pop();
				Object container = stack.pop();
				stack.push(this.elem(key, container));
				++pc;
				break;
			}
			case LOAD_VAR: {
				Object v = vars.get(pool[code.arg1]);
				stack.push(v);
				++pc;
				break;
			}
			case SET_VAR: {
				// vars[pool[arg1]]=POP()
				Object tos = stack.pop();
				vars.put((String) pool[code.arg1], tos);
				++pc;
				break;
			}
			case MAKE_ARRAY: {
				LinkedList<Object> list = new LinkedList<Object>();
				for (int i = 0; i < code.arg1; ++i) {
					list.addFirst(stack.pop());
				}
				stack.push(list);
				++pc;
				break;
			}
			case RETURN:
				return buffer.toString();
			case ITER_START: {
				// Get an iterator object from TOS.
				// Put iterator object on the loop stack.

				Object container = stack.pop();

				if (container instanceof Collection) {
					// TODO better casting
					@SuppressWarnings("unchecked")
					Iterator<Object> iterator = ((Collection<Object>) container)
							.iterator();
					Loop loop = new Loop(iterator, pc);
					loopStack.push(loop);
					stack.push(loop.next());

					++pc;
				} else {
					throw new RuntimeException(
							"Non container type detected in FOREACH.");
				}
				break;
			}
			case FOR_ITER: {
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

				break;
			}
			case JUMP: {
				pc += code.arg1;
				break;
			}
			case JUMP_ABS: {
				pc = code.arg1;
				break;
			}
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
				Irep compiledIrep = loader.compile(Paths.get(path), compiler);
				String result = this.run(compiledIrep, vars);
				stack.push(result);
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
			default:
				throw new RuntimeException("SHOULD NOT REACH HERE: " + code.op);
			}
		}
	}

	private Object doMethodCall(int paramCount, Stack<Object> stack) {
		Object[] params = new Object[paramCount];
		for (int i = 0; i < paramCount; ++i) {
			params[paramCount - i - 1] = stack.pop();
		}

		Object methodName = stack.pop();
		Object object = stack.pop();
		// Lookup lookup = MethodHandles.lookup();
		// MethodHandle methodBody = lookup.findVirtual(object.getClass(),
		// ident.toString(),
		// MethodType.methodType(object.getClass(), parameterTypes));
		// Object retval = methodBody.invokeWithArguments(params);
		// Method method = object.getClass().getMethod(ident.toString(),
		// parameterTypes);
		// Object retval = method.invoke(object, params);
		MethodAccess access = MethodAccess.get(object.getClass());
		Object retval = access.invoke(object, methodName.toString(), params);
		return retval;
		/*
		 * e.printStackTrace(); if (this.strictMode) { throw new
		 * JTTRuntimeError("Oops... : " + e.getStackTrace()); } else {
		 * warn("Can't call method: " + e.getStackTrace()); return null; }
		 */
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

	private void doMakeRange(Stack<Object> stack) throws JSlateException {
		Object lhs = stack.pop();
		Object rhs = stack.pop();
		if (!(lhs instanceof Integer)) {
			throw new JSlateException(
					"Left side of range construction operator should be Integer but : "
							+ lhs.getClass());
		}
		if (!(rhs instanceof Integer)) {
			throw new JSlateException(
					"Right side of range construction operator should be Integer but : "
							+ rhs.getClass());
		}

		int l = ((Integer) lhs).intValue();
		int r = ((Integer) rhs).intValue();
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = l; i <= r; ++i) {
			list.add(i);
		}
		stack.push(list);
	}

	private void doFuncall(int arglen, Stack<Object> stack) {
		Object method = stack.get(stack.size() - arglen);
		if (method instanceof String) {
			Function function = this.functions.get(method);
			if (function != null) {
				Object[] objects = new Object[arglen];
				for (int i = 0; i < arglen + 1; ++i) {
					objects[i] = stack.pop();
				}
				function.call(objects);
			} else {
				warn("Unknown function: " + method);
				for (int i = 0; i < arglen + 1; ++i) {
					stack.pop();
				}
				stack.push(null);
			}
		} else {
			throw new RuntimeException("NIY");
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
			return list.get((Integer)index);
		} else if (container == null) {
			warn("container is null");
			return null;
		} else {
			warn("Unsupported container: " + container.getClass());
			// TODO: s
			return null;
		}
	}

	private String doConcat(Object lhs, Object rhs) {
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

	private void warn(String string) {
		// We should provide a WarningListener interface.
		// TODO We should show line number.
		System.out.println(string);
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

	private Object elem(Object key, Object container) throws TypeException {
		if (container instanceof Map) {
			return ((Map<?, ?>) container).get(key);
		} else if (container instanceof List) {
			return ((List<?>) container).get((Integer) key);
		} else {
			throw new TypeException("Container must be List or Map: "
					+ container.getClass());
		}
	}

	// TODO support arguments
	private Object invokeMethod(Object methodName, Object self, int numargs)
			throws JSlateException {
		assert methodName instanceof String;

		Lookup lookup = MethodHandles.lookup();
		try {
			MethodHandle meth;
			meth = lookup.findVirtual(self.getClass(), (String) methodName,
					MethodType.methodType(self.getClass()));
			Object retval = meth.invokeWithArguments(self);
			return retval;
		} catch (Throwable e) {
			throw new MethodInvokeException(e);
		}
	}

	private Object doAdd(Object lhs, Object rhs) throws TypeException {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						+ ((Integer) rhs).intValue());
			} else {
				throw new TypeException("rhs for '+' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						+ ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						+ ((Double) rhs).doubleValue());
			} else {
				throw new TypeException("rhs for '+' must be Number");
			}
		} else {
			throw new TypeException("lhs for '+' must be Number");
		}
	}

	private Object doSubtract(Object lhs, Object rhs) throws TypeException {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						- ((Integer) rhs).intValue());
			} else {
				throw new TypeException("rhs for '-' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						- ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						- ((Double) rhs).doubleValue());
			} else {
				throw new TypeException("rhs for '-' must be Number");
			}
		} else {
			throw new TypeException("lhs for '-' must be Number");
		}
	}

	private Object doMultiply(Object lhs, Object rhs) throws TypeException {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						* ((Integer) rhs).intValue());
			} else {
				throw new TypeException("rhs for '*' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						* ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						* ((Double) rhs).doubleValue());
			} else {
				throw new TypeException("rhs for '*' must be Number");
			}
		} else {
			throw new TypeException("lhs for '*' must be Number");
		}
	}

	private Object doDivide(Object lhs, Object rhs) throws TypeException {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						/ ((Integer) rhs).intValue());
			} else {
				throw new TypeException("rhs for '/' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						/ ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						/ ((Double) rhs).doubleValue());
			} else {
				throw new TypeException("rhs for '/' must be Number");
			}
		} else {
			throw new TypeException("lhs for '/' must be Number");
		}
	}

	private Object doModulo(Object lhs, Object rhs) throws TypeException {
		if (lhs instanceof Integer) {
			if (rhs instanceof Integer) {
				return Integer.valueOf(((Integer) lhs).intValue()
						% ((Integer) rhs).intValue());
			} else {
				throw new TypeException("rhs for '+' must be Number");
			}
		} else if (lhs instanceof Double) {
			if (rhs instanceof Integer) {
				return Double.valueOf(((Double) lhs).doubleValue()
						% ((Integer) rhs).intValue());
			} else if (rhs instanceof Double) {
				return Double.valueOf(((Double) lhs).doubleValue()
						% ((Double) rhs).doubleValue());
			} else {
				throw new TypeException("rhs for '+' must be Number");
			}
		} else {
			throw new TypeException("lhs for '+' must be Number");
		}
	}

	private Boolean doEqauls(Object lhs, Object rhs) {
		if (lhs == null) {
			return rhs == null;
		}
		return new Boolean(lhs.equals(rhs));
	}

	private Object doNotEqauls(Object lhs, Object rhs) {
		if (lhs == null) {
			return rhs != null;
		}
		return new Boolean(! lhs.equals(rhs));
	}


	// lhs > rhs
	private Object doGT(Object lhs, Object rhs) throws TypeException {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret > 0;
		} else {
			throw new TypeException("lhs for '>' must implement Comparable");
		}
	}

	// lhs >= rhs
	boolean doGE(Object lhs, Object rhs) throws TypeException {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret >= 0;
		} else {
			throw new TypeException("lhs for '>' must implement Comparable");
		}
	}

	private Object doLT(Object lhs, Object rhs) throws TypeException {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret < 0;
		} else {
			throw new TypeException("lhs for '<' must implement Comparable");
		}
	}

	private Object doLE(Object lhs, Object rhs) throws TypeException {
		// TODO better casting
		if (lhs instanceof Comparable) {
			@SuppressWarnings("unchecked")
			int ret = ((Comparable<Object>) lhs).compareTo(rhs);
			return ret <= 0;
		} else {
			throw new TypeException("lhs for '<=' must implement Comparable");
		}
	}

}
