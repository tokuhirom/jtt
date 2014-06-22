package me.geso.jtt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import me.geso.jtt.exception.JTTCompilerError;
import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.vm.Code;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.IrepBuilder;
import me.geso.jtt.vm.OP;

class Visitor {
	private final IrepBuilder builder;
	private final Stack<List<Code>> lastStack = new Stack<>();
	private final Stack<Map<String, Integer>> lvarStack = new Stack<>();
	private final Stack<List<Code>> nextStack = new Stack<>();
	private int lvarIndex = 0;
	private int regIndex = 0;

	public Visitor(Source source) {
		this.builder = new IrepBuilder(source);
	}

	public int declareLocalVariable(String name) {
		Map<String, Integer> lastElement = lvarStack.lastElement();
		int idx = lvarIndex++;
		lastElement.put(name, idx);
		return idx;
	}

	public Integer getLocalVariableIndex(String name) {
		for (int i = 0; i < lvarStack.size(); ++i) {
			Map<String, Integer> vars = lvarStack.get(i);
			Iterator<String> iterator = vars.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				if (key.equals(name)) {
					return vars.get(key);
				}
			}
		}
		return null;
	}

	private void visitAst(Node node, int reg) {
		switch (node.getType()) {
		case EXPRESSION: {
			int a = this.reserveReg();
			for (int i = 0; i < node.getChildren().size(); ++i) {
				visitAst(node.getChildren().get(i), a);
			}
			if (node.getChildren().get(0).getType() != NodeType.SET) {
				builder.add(OP.APPEND, a, node);
			}
			return;
		}
		case TEMPLATE:
			for (Node n : node.getChildren()) {
				visitAst(n, reg);
			}
			return;
		case ARRAY: {
			if (node.getChildren().size() > 0) {
				int[] srcRegs = new int[node.getChildren().size()];
				for (int i = 0; i < node.getChildren().size(); ++i) {
					srcRegs[i] = this.reserveReg();
				}

				for (int i = 0; i < node.getChildren().size(); ++i) {
					visitAst(node.getChildren().get(i), srcRegs[i]);
				}
				builder.add(OP.MAKE_ARRAY, srcRegs[0], node.getChildren()
						.size(), node);
				builder.add(OP.MOVE, reg, srcRegs[0], node);
				return;
			} else {
				builder.add(OP.MAKE_ARRAY, reg, 0, node);
			}
			return;
		}
		case ADD:
			compileBinOp(node, OP.ADD, reg);
			return;
		case SUBTRACT:
			compileBinOp(node, OP.SUBTRACT, reg);
			return;
		case MULTIPLY:
			compileBinOp(node, OP.MULTIPLY, reg);
			return;
		case DIVIDE:
			compileBinOp(node, OP.DIVIDE, reg);
			return;
		case MODULO:
			compileBinOp(node, OP.MODULO, reg);
			return;
		case EQUALS:
			compileBinOp(node, OP.EQUALS, reg);
			return;
		case GT:
			compileBinOp(node, OP.GT, reg);
			return;
		case GE:
			compileBinOp(node, OP.GE, reg);
			return;
		case LT:
			compileBinOp(node, OP.LT, reg);
			return;
		case LE:
			compileBinOp(node, OP.LE, reg);
			return;
		case NE:
			compileBinOp(node, OP.NE, reg);
			return;
		case CONCAT:
			compileBinOp(node, OP.CONCAT, reg);
			return;
		case ANDAND:
			compileBinOp(node, OP.ANDAND, reg);
			return;
		case OROR:
			compileBinOp(node, OP.OROR, reg);
			return;
		case RAW_STRING:
			builder.addPool(OP.APPEND_RAW, node.getText(), node);
			return;
		case INTEGER:
			if (reg == -1) {
				throw new JTTError("'integer' in void context");
			}
			builder.add(OP.LOAD_INT, Integer.valueOf(node.getText()), reg, node);
			return;
		case DOUBLE:
			if (reg == -1) {
				throw new JTTError("'double' in void context");
			}
			builder.addPool(OP.LOAD_CONST, Double.valueOf(node.getText()), reg,
					node);
			return;
		case LOOP:
			builder.add(OP.LOOP, reg, node);
			return;
		case TRUE:
			if (reg == -1) {
				throw new JTTError("'true' in void context");
			}
			builder.add(OP.LOAD_TRUE, reg, node);
			return;
		case FALSE:
			builder.add(OP.LOAD_FALSE, reg, node);
			return;
		case NULL:
			builder.add(OP.LOAD_NULL, reg, node);
			return;
		case SET: {
			// (set ident expr)
			Node ident = node.getChildren().get(0);
			Node expr = node.getChildren().get(1);
			// int a = this.reserveReg();
			// visitAst(expr, a);
			// int lvar = this.declareLocalVariable(ident.getText());
			// builder.addPool(OP.SET_LVAR, lvar, a, node);
			int a = this.reserveReg();
			visitAst(expr, a);
			builder.addPool(OP.SET_VAR, ident.getText(), a, node);
			return;
		}
		case IF: {
			// (if cond body else)
			List<Node> children = node.getChildren();
			Node condClause = children.get(0);
			Node bodyClause = children.get(1);
			Node elseClause = children.get(2);

			int a = this.reserveReg();
			visitAst(condClause, a);
			Code condJump = builder.addLazy(OP.JUMP_IF_FALSE, a, condClause);
			int pos = builder.getSize();
			visitAst(bodyClause, reg);
			if (elseClause != null) {
				Code code = builder.addLazy(OP.JUMP, elseClause);

				condJump.b = builder.getSize() - pos + 1;
				int elseStart = builder.getSize();
				visitAst(elseClause, reg);
				code.a = builder.getSize() - elseStart + 1;
			} else {
				condJump.b = builder.getSize() - pos + 1;
			}
			return;
		}
		case SWITCH: {
			// (template (switch (case (integer 1) (template (raw_string
			// a))) (case (integer 2) (template (raw_string b))) (default
			// (template (raw_string c)))))"
			ArrayList<Code> gotoLast = new ArrayList<>();

			int varReg = this.reserveReg();
			int tmpReg = this.reserveReg();

			// first argument for switch stmt.
			visitAst(node.getChildren().get(0), varReg);

			for (int i = 1; i < node.getChildren().size(); ++i) {
				Node n = node.getChildren().get(i);
				// expr maybe null. `[% CASE %]` is valid.
				if (n.getType() == NodeType.CASE) {
					if (n.getChildren().get(0) != null) {
						// condition
						int b = this.reserveReg();
						visitAst(n.getChildren().get(0), b);
						builder.add(OP.MOVE, tmpReg, varReg, n);
						builder.add(OP.MATCH, tmpReg, b, n);
						Code jmp = builder.addLazy(OP.JUMP_IF_FALSE, tmpReg, n);
						int pos = builder.getSize();
						// body
						visitAst(n.getChildren().get(1), -1);
						gotoLast.add(builder.addLazy(OP.JUMP_ABS, n));
						jmp.b = builder.getSize() - pos + 1;
					} else {
						visitAst(n.getChildren().get(1), -1);
						gotoLast.add(builder.addLazy(OP.JUMP_ABS, n));
					}
				} else {
					throw new RuntimeException("SHOULD NOT REACH HERE");
				}
			}
			for (Code c : gotoLast) {
				c.a = builder.getSize();
			}
			return;
		}
		case RANGE: {
			assert node.getChildren().size() == 2;
			Node lhs = node.getChildren().get(0);
			Node rhs = node.getChildren().get(1);
			visitAst(lhs, reg);
			int b = this.reserveReg();
			visitAst(rhs, b);
			builder.add(OP.MAKE_RANGE, reg, b, node);
			return;
		}
		case NOT: {
			visitAst(node.getChildren().get(0), reg);
			builder.add(OP.NOT, reg, node);
			return;
		}
		case IDENT: {
			if (reg == -1) {
				throw new JTTError("ident in void context.");
			}
			Integer idx = this.getLocalVariableIndex(node.getText());
			if (idx != null) {
				// local variable
				builder.add(OP.LOAD_LVAR, idx, reg, node);
			} else {
				// global vars(maybe passed from external world)
				builder.addPool(OP.LOAD_VAR, node.getText(), reg, node);
			}
			return;
		}
		case DOLLARVAR: {
			if (reg == -1) {
				throw new JTTError("ident in void context.");
			}

			Integer idx = this.getLocalVariableIndex(node.getText());
			if (idx != null) {
				// local variable
				builder.add(OP.LOAD_LVAR, idx, reg, node);
			} else {
				// global vars(maybe passed from external world)
				builder.addPool(OP.LOAD_VAR, node.getText(), reg, node);
			}
			return;
		}
		case STRING:
			if (reg == -1) {
				throw new JTTError("'string' in void context");
			}
			builder.addPool(OP.LOAD_CONST, node.getText(), reg, node);
			return;
		case FOREACH: { // [% FOR x IN y %]
			assert node.getChildren().size() == 3;

			Node var = node.getChildren().get(0);
			Node container = node.getChildren().get(1);
			Node body = node.getChildren().get(2);

			int containerReg = this.reserveReg();
			visitAst(container, containerReg);
			builder.add(OP.ITER_START, containerReg, node);
			builder.increaseLoopStackSize();

			int lvar = declareLocalVariable(var.getText());
			builder.add(OP.SET_LVAR, lvar, containerReg, node);

			nextStack.add(new ArrayList<Code>());
			lastStack.add(new ArrayList<Code>());

			visitAst(body, -1);

			int nextPos = builder.getSize();
			builder.add(OP.FOR_ITER, containerReg, node);
			int lastPos = builder.getSize();

			for (Code code : lastStack.lastElement()) {
				code.a = lastPos;
			}
			lastStack.pop();

			for (Code code : nextStack.lastElement()) {
				code.a = nextPos;
			}
			nextStack.pop();

			return;
		}
		case WHILE: {
			// (while expr body)
			Node expr = node.getChildren().get(0);
			Node body = node.getChildren().get(1);

			int pos = builder.getSize();

			int nextPos = builder.getSize();

			int exprReg = this.reserveReg();
			visitAst(expr, exprReg);

			Code jumpAfterCond = builder.addLazy(OP.JUMP_IF_FALSE, exprReg,
					expr);
			int afterExprPos = builder.getSize();

			nextStack.add(new ArrayList<Code>());
			lastStack.add(new ArrayList<Code>());

			visitAst(body, -1);

			builder.add(OP.JUMP, pos - builder.getSize(), node);
			jumpAfterCond.b = builder.getSize() - afterExprPos + 1;

			for (Code code : lastStack.lastElement()) {
				code.a = builder.getSize();
			}
			lastStack.pop();

			for (Code code : nextStack.lastElement()) {
				code.a = nextPos;
			}
			nextStack.pop();

			return;
		}
		case NEXT: {
			Code code = builder.addLazy(OP.JUMP_ABS, node);
			nextStack.lastElement().add(code);
			return;
		}
		case LAST: {
			Code code = builder.addLazy(OP.JUMP_ABS, node);
			lastStack.lastElement().add(code);
			return;
		}
		case INCLUDE: {
			int a = this.reserveReg();
			visitAst(node.getChildren().get(0), a);
			builder.add(OP.INCLUDE, a, node);
			builder.add(OP.APPEND, a, node);
			return;
		}
		case ATTRIBUTE: {
			Node key = node.getChildren().get(0);
			Node val = node.getChildren().get(1);

			visitAst(key, reg);
			int b = this.reserveReg();
			visitAst(val, b);

			builder.add(OP.ATTRIBUTE, reg, b, node);
			return;
		}
		case MAP: {
			List<Node> children = node.getChildren();
			int[] regs = new int[children.size()];
			for (int i = 0; i < children.size(); i++) {
				regs[i] = this.reserveReg();
			}
			for (int i = 0; i < children.size(); i += 2) {
				Node key = children.get(i);
				Node value = children.get(i + 1);
				if (key.getType() == NodeType.IDENT) {
					builder.addPool(OP.LOAD_CONST, key.getText(), regs[i], node);
				} else {
					visitAst(key, regs[i]);
				}
				visitAst(value, regs[i + 1]);
			}

			builder.add(OP.MAKE_MAP, regs[0], children.size(), node);
			builder.add(OP.MOVE, reg, regs[0], node);
			return;
		}
		case FUNCALL: {
			Node func = node.getChildren().get(0);
			if (func.getType() == NodeType.IDENT) {
				if (func.getText().equals("lc")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'lc'");
					}
					visitAst(node.getChildren().get(1), reg);
					builder.add(OP.LC, reg, func);
					return;
				} else if (func.getText().equals("uc")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'uc'");
					}
					visitAst(node.getChildren().get(1), reg);
					builder.add(OP.UC, reg, func);
					return;
				} else if (func.getText().equals("uri")
						|| func.getText().equals("url")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'ur[il]'");
					}
					visitAst(node.getChildren().get(1), reg);
					builder.add(OP.URI_ESCAPE, reg, func);
					return;
				} else if (func.getText().equals("sprintf")) {
					int[] regs = new int[node.getChildren().size()];
					for (int i = 0; i < node.getChildren().size() - 1; ++i) {
						regs[i] = this.reserveReg();
					}
					for (int i = 1; i < node.getChildren().size(); ++i) {
						visitAst(node.getChildren().get(i), regs[i - 1]);
					}
					builder.add(OP.SPRINTF, regs[0],
							node.getChildren().size() - 1, func);
					builder.add(OP.MOVE, reg, regs[0], func);
					return;
				} else {
					// [% lc(3) %]
					// (funcall (ident twice) (integer 3))
					int[] regs = new int[node.getChildren().size()];
					for (int i = 0; i < node.getChildren().size(); ++i) {
						regs[i] = this.reserveReg();
					}
					builder.addPool(OP.LOAD_CONST, func.getText(), regs[0], node);
					for (int i = 1; i < node.getChildren().size(); ++i) {
						visitAst(node.getChildren().get(i), regs[i]);
					}
					builder.add(OP.FUNCALL, regs[0],
							node.getChildren().size() - 1, node);
					builder.add(OP.MOVE, reg, regs[0], node);
					return;
				}
			} else if (func.getType() == NodeType.ATTRIBUTE) {
				// (funcall (attribute (array (integer 5) (integer 9) (integer
				// 6) (integer 3)) (string size)))
				// (funcall (attribute (string hoge) (string substring))
				// (integer 2))
				int[] regs = new int[node.getChildren().size() + 1];
				for (int i = 0; i < node.getChildren().size() + 1; ++i) {
					regs[i] = this.reserveReg();
				}
				visitAst(func.getChildren().get(0), regs[0]); // object
				builder.addPool(OP.LOAD_CONST, func.getChildren().get(1)
						.getText(), regs[1], node); // method name
				for (int i = 1; i < node.getChildren().size(); ++i) {
					visitAst(node.getChildren().get(i), regs[i + 1]);
				}
				builder.add(OP.METHOD_CALL, regs[0],
						node.getChildren().size() - 1, node);
				builder.add(OP.MOVE, reg, regs[0], node);
			} else {
				throw new RuntimeException("Invalid funcall : "
						+ func.getType());
				// builder.add(OP.FUNCALL, node.getChildren().size() - 1);
			}
			return;
		}
		case WRAPPER: {
			// (template (wrapper (string foo.tt) (template (raw_string
			// ohoho))))
			String fileName = node.getChildren().get(0).getText();
			Node body = node.getChildren().get(1);

			visitAst(body, -1);
			int a = this.reserveReg();
			builder.addPool(OP.LOAD_CONST, fileName, a, node);
			builder.add(OP.WRAP, a, node);

			return;
		}
		case DEFAULT:
		case CASE:
			// Because SWITCH handler eats the node.
			throw new RuntimeException("Should not reach here: "
					+ node.getType());
		}

		throw new RuntimeException("Should not reach here: " + node.getType());
	}

	private int reserveReg() {
		int r = regIndex;
		++regIndex;
		return r;
	}

	private void compileBinOp(Node node, OP op, int reg)
			throws JTTCompilerError {
		assert node.getChildren().size() == 2;
		visitAst(node.getChildren().get(0), reg);
		int b = this.reserveReg();
		visitAst(node.getChildren().get(1), b);
		builder.add(op, reg, b, node);
	}

	public Irep getResult() {
		builder.addReturn();
		return builder.build(this.lvarIndex + 1);
	}

	public void start(Node ast) throws JTTCompilerError {
		lvarStack.push(new HashMap<>());
		this.visitAst(ast, -1);
	}
}

public class Compiler {

	public Irep compile(Source source, Node ast) throws ParserError,
			JTTCompilerError {
		if (ast == null) {
			throw new IllegalArgumentException("ast must not be null");
		}
		Visitor visitor = new Visitor(source);
		visitor.start(ast);
		return visitor.getResult();
	}
}
