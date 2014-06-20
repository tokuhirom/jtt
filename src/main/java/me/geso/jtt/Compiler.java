package me.geso.jtt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import me.geso.jtt.exception.JTTCompilerError;
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

	private void visitAst(Node node) {
		switch (node.getType()) {
		case EXPRESSION:
			for (Node n : node.getChildren()) {
				visitAst(n);
			}
			if (node.getChildren().get(0).getType() != NodeType.SET) {
				builder.add(OP.APPEND, node);
			}
			break;
		case TEMPLATE:
			for (Node n : node.getChildren()) {
				visitAst(n);
			}
			break;
		case ARRAY:
			for (Node n : node.getChildren()) {
				visitAst(n);
			}
			builder.add(OP.MAKE_ARRAY, node.getChildren().size(), node);
			break;
		case ADD:
			compileBinOp(node, OP.ADD);
			break;
		case SUBTRACT:
			compileBinOp(node, OP.SUBTRACT);
			break;
		case MULTIPLY:
			compileBinOp(node, OP.MULTIPLY);
			break;
		case DIVIDE:
			compileBinOp(node, OP.DIVIDE);
			break;
		case MODULO:
			compileBinOp(node, OP.MODULO);
			break;
		case EQUALS:
			compileBinOp(node, OP.EQUALS);
			break;
		case GT:
			compileBinOp(node, OP.GT);
			break;
		case GE:
			compileBinOp(node, OP.GE);
			break;
		case LT:
			compileBinOp(node, OP.LT);
			break;
		case LE:
			compileBinOp(node, OP.LE);
			break;
		case NE:
			compileBinOp(node, OP.NE);
			break;
		case CONCAT:
			compileBinOp(node, OP.CONCAT);
			break;
		case ANDAND:
			compileBinOp(node, OP.ANDAND);
			break;
		case OROR:
			compileBinOp(node, OP.OROR);
			break;
		case RAW_STRING:
			builder.addPool(OP.APPEND_RAW, node.getText(), node);
			break;
		case INTEGER:
			builder.addPool(OP.LOAD_CONST, Integer.valueOf(node.getText()),
					node);
			break;
		case DOUBLE:
			builder.addPool(OP.LOAD_CONST, Double.valueOf(node.getText()), node);
			break;
		case LOOP:
			builder.add(OP.LOOP, node);
			break;
		case TRUE:
			builder.add(OP.LOAD_TRUE, node);
			break;
		case FALSE:
			builder.add(OP.LOAD_FALSE, node);
			break;
		case NULL:
			builder.add(OP.LOAD_NULL, node);
			break;
		case SET: {
			// (set ident expr)
			Node ident = node.getChildren().get(0);
			Node expr = node.getChildren().get(1);
			visitAst(expr);
			builder.addPool(OP.SET_VAR, ident.getText(), node);
			break;
		}
		case IF: {
			// (if cond body else)
			List<Node> children = node.getChildren();
			Node condClause = children.get(0);
			Node bodyClause = children.get(1);
			Node elseClause = children.get(2);

			visitAst(condClause);
			Code condJump = builder.addLazy(OP.JUMP_IF_FALSE, condClause);
			int pos = builder.getSize();
			visitAst(bodyClause);
			if (elseClause != null) {
				Code code = builder.addLazy(OP.JUMP, elseClause);

				condJump.arg1 = builder.getSize() - pos + 1;
				int elseStart = builder.getSize();
				visitAst(elseClause);
				code.arg1 = builder.getSize() - elseStart + 1;
			} else {
				condJump.arg1 = builder.getSize() - pos + 1;
			}
			break;
		}
		case SWITCH: {
			// (template (switch (case (integer 1) (template (raw_string
			// a))) (case (integer 2) (template (raw_string b))) (default
			// (template (raw_string c)))))"
			ArrayList<Code> gotoLast = new ArrayList<>();

			// first argument for switch stmt.
			visitAst(node.getChildren().get(0));
			int lvar = declareLocalVariable(null);
			builder.add(OP.SET_LVAR, lvar, node);

			for (int i = 1; i < node.getChildren().size(); ++i) {
				Node n = node.getChildren().get(i);
				// expr maybe null. `[% CASE %]` is valid.
				if (n.getType() == NodeType.CASE) {
					if (n.getChildren().get(0) != null) {
						// condition
						builder.add(OP.LOAD_LVAR, lvar, n);
						visitAst(n.getChildren().get(0));
						builder.add(OP.MATCH, n);
						Code jmp = builder.addLazy(OP.JUMP_IF_FALSE, n);
						int pos = builder.getSize();
						// body
						visitAst(n.getChildren().get(1));
						gotoLast.add(builder.addLazy(OP.JUMP_ABS, n));
						jmp.arg1 = builder.getSize() - pos + 1;
					} else {
						visitAst(n.getChildren().get(1));
						gotoLast.add(builder.addLazy(OP.JUMP_ABS, n));
					}
				} else {
					throw new RuntimeException("SHOULD NOT REACH HERE");
				}
			}
			for (Code c : gotoLast) {
				c.arg1 = builder.getSize();
			}
			break;
		}
		case RANGE: {
			assert node.getChildren().size() == 2;
			Node lhs = node.getChildren().get(0);
			Node rhs = node.getChildren().get(1);
			visitAst(lhs);
			visitAst(rhs);
			builder.add(OP.MAKE_RANGE, node);
			break;
		}
		case NOT: {
			visitAst(node.getChildren().get(0));
			builder.add(OP.NOT, node);
			break;
		}
		case IDENT: {
			Integer idx = this.getLocalVariableIndex(node.getText());
			if (idx != null) {
				// local variable
				builder.add(OP.LOAD_LVAR, idx, node);
			} else {
				// global vars(maybe passed from external world)
				builder.addPool(OP.LOAD_VAR, node.getText(), node);
			}
			break;
		}
		case STRING:
			builder.addPool(OP.LOAD_CONST, node.getText(), node);
			break;
		case FOREACH: { // [% FOR x IN y %]
			assert node.getChildren().size() == 3;

			Node var = node.getChildren().get(0);
			Node container = node.getChildren().get(1);
			Node body = node.getChildren().get(2);

			visitAst(container);
			builder.add(OP.ITER_START, node);

			int lvar = declareLocalVariable(var.getText());
			builder.add(OP.SET_LVAR, lvar, node);

			nextStack.add(new ArrayList<Code>());
			lastStack.add(new ArrayList<Code>());

			visitAst(body);

			int nextPos = builder.getSize();
			builder.add(OP.FOR_ITER, node);
			int lastPos = builder.getSize();

			for (Code code : lastStack.lastElement()) {
				code.arg1 = lastPos;
			}
			lastStack.pop();

			for (Code code : nextStack.lastElement()) {
				code.arg1 = nextPos;
			}
			nextStack.pop();

			break;
		}
		case WHILE: {
			// (while expr body)
			Node expr = node.getChildren().get(0);
			Node body = node.getChildren().get(1);

			int pos = builder.getSize();

			int nextPos = builder.getSize();

			visitAst(expr);

			Code jumpAfterCond = builder.addLazy(OP.JUMP_IF_FALSE, expr);
			int afterExprPos = builder.getSize();

			nextStack.add(new ArrayList<Code>());
			lastStack.add(new ArrayList<Code>());

			visitAst(body);

			builder.add(OP.JUMP, pos - builder.getSize(), node);
			jumpAfterCond.arg1 = builder.getSize() - afterExprPos + 1;

			for (Code code : lastStack.lastElement()) {
				code.arg1 = builder.getSize();
			}
			lastStack.pop();

			for (Code code : nextStack.lastElement()) {
				code.arg1 = nextPos;
			}
			nextStack.pop();

			break;
		}
		case NEXT: {
			Code code = builder.addLazy(OP.JUMP_ABS, node);
			nextStack.lastElement().add(code);
			break;
		}
		case LAST: {
			Code code = builder.addLazy(OP.JUMP_ABS, node);
			lastStack.lastElement().add(code);
			break;
		}
		case INCLUDE: {
			visitAst(node.getChildren().get(0));
			builder.add(OP.INCLUDE, node);
			builder.add(OP.APPEND, node);
			break;
		}
		case ATTRIBUTE: {
			Node key = node.getChildren().get(0);
			Node val = node.getChildren().get(1);

			visitAst(key);
			visitAst(val);

			builder.add(OP.ATTRIBUTE, node);
			break;
		}
		case MAP: {
			List<Node> children = node.getChildren();
			for (int i = 0; i < children.size(); i += 2) {
				Node key = children.get(i);
				Node value = children.get(i + 1);
				if (key.getType() == NodeType.IDENT) {
					builder.addPool(OP.LOAD_CONST, key.getText(), node);
				} else {
					visitAst(key);
				}
				visitAst(value);
			}

			builder.add(OP.MAKE_MAP, children.size() / 2, node);
			break;
		}
		case DOLLARVAR: {
			builder.addPool(OP.LOAD_VAR, node.getText(), node);
			break;
		}
		case FUNCALL: {
			Node func = node.getChildren().get(0);
			if (func.getType() == NodeType.IDENT) {
				// [% lc(3) %]
				for (int i = 1; i < node.getChildren().size(); ++i) {
					visitAst(node.getChildren().get(i));
				}
				if (func.getText().equals("lc")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'lc'");
					}
					builder.add(OP.LC, node.getChildren().size() - 1, func);
				} else if (func.getText().equals("uc")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'uc'");
					}
					builder.add(OP.UC, node.getChildren().size() - 1, node);
				} else if (func.getText().equals("sprintf")) {
					builder.add(OP.SPRINTF, node.getChildren().size() - 1, node);
				} else if (func.getText().equals("uri")
						|| func.getText().equals("url")) {
					if (node.getChildren().size() - 1 != 1) {
						throw new JTTCompilerError(
								"Invalid argument count for 'ur[il]'");
					}
					builder.add(OP.URI_ESCAPE, node.getChildren().size() - 1,
							node);
				} else {
					builder.addPool(OP.LOAD_CONST, func.getText(), node);
					builder.add(OP.FUNCALL, node.getChildren().size() - 1, node);
				}
			} else if (func.getType() == NodeType.ATTRIBUTE) {
				visitAst(func.getChildren().get(0)); // object
				builder.addPool(OP.LOAD_CONST, func.getChildren().get(1)
						.getText(), node); // method name
				for (int i = 1; i < node.getChildren().size(); ++i) {
					visitAst(node.getChildren().get(i));
				}
				builder.add(OP.METHOD_CALL, node.getChildren().size() - 1, node);
			} else {
				throw new RuntimeException("Invalid funcall : "
						+ func.getType());
				// builder.add(OP.FUNCALL, node.getChildren().size() - 1);
			}
			break;
		}
		case WRAPPER: {
			// (template (wrapper (string foo.tt) (template (raw_string
			// ohoho))))
			String fileName = node.getChildren().get(0).getText();
			Node body = node.getChildren().get(1);

			visitAst(body);

			builder.addPool(OP.LOAD_CONST, fileName, node);
			builder.add(OP.WRAP, node);

			break;
		}
		default:
			throw new RuntimeException("Should not reach here: "
					+ node.getType());
		}
	}

	void compileBinOp(Node node, OP op) throws JTTCompilerError {
		assert node.getChildren().size() == 2;
		for (int i = node.getChildren().size() - 1; i >= 0; --i) {
			visitAst(node.getChildren().get(i));
		}
		builder.add(op, node);
	}

	public Irep getResult() {
		builder.addReturn();
		return builder.build(this.lvarIndex+1);
	}

	public void start(Node ast) throws JTTCompilerError {
		lvarStack.push(new HashMap<>());
		this.visitAst(ast);
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
