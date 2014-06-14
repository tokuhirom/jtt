package me.geso.jtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import me.geso.jtt.exception.JTTCompilerError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.vm.Code;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.IrepBuilder;
import me.geso.jtt.vm.OP;

public class Compiler {

	class Visitor {
		private IrepBuilder builder = new IrepBuilder();
		private Stack<List<Code>> lastStack = new Stack<>();
		private Stack<List<String>> lvarStack = new Stack<>();
		private Stack<List<Code>> nextStack = new Stack<>();

		public int reserveLocalVariable(String name) {
			lvarStack.lastElement().add(name);
			return lvarStack.lastElement().size() - 1;
		}

		private void visitAst(Node node) {
			switch (node.getType()) {
			case EXPRESSION:
				for (Node n : node.getChildren()) {
					visitAst(n);
				}
				if (node.getChildren().get(0).getType() != NodeType.SET) {
					builder.add(OP.APPEND);
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
				builder.add(OP.MAKE_ARRAY, node.getChildren().size());
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
				builder.addPool(OP.LOAD_CONST, new JTTRawString(node.getText()));
				builder.add(OP.APPEND);
				break;
			case INTEGER:
				builder.addPool(OP.LOAD_CONST, Integer.valueOf(node.getText()));
				break;
			case DOUBLE:
				builder.addPool(OP.LOAD_CONST, Double.valueOf(node.getText()));
				break;
			case LOOP:
				builder.add(OP.LOOP);
				break;
			case TRUE:
				builder.add(OP.LOAD_TRUE);
				break;
			case FALSE:
				builder.add(OP.LOAD_FALSE);
				break;
			case NULL:
				builder.add(OP.LOAD_NULL);
				break;
			case SET: {
				// (set ident expr)
				Node ident = node.getChildren().get(0);
				Node expr = node.getChildren().get(1);
				visitAst(expr);
				builder.addPool(OP.SET_VAR, ident.getText());
				break;
			}
			case IF: {
				// (if cond body else)
				List<Node> children = node.getChildren();
				Node condClause = children.get(0);
				Node bodyClause = children.get(1);
				Node elseClause = children.get(2);

				visitAst(condClause);
				Code condJump = builder.addLazy(OP.JUMP_IF_FALSE);
				int pos = builder.getSize();
				visitAst(bodyClause);
				if (elseClause != null) {
					Code code = builder.addLazy(OP.JUMP);

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
				int lvar = reserveLocalVariable(null);
				builder.add(OP.SET_LVAR, lvar);

				for (int i = 1; i < node.getChildren().size(); ++i) {
					Node n = node.getChildren().get(i);
					// expr maybe null. `[% CASE %]` is valid.
					if (n.getType() == NodeType.CASE) {
						if (n.getChildren().get(0) != null) {
							// condition
							builder.add(OP.LOAD_LVAR, lvar);
							visitAst(n.getChildren().get(0));
							builder.add(OP.MATCH);
							Code jmp = builder.addLazy(OP.JUMP_IF_FALSE);
							int pos = builder.getSize();
							// body
							visitAst(n.getChildren().get(1));
							gotoLast.add(builder.addLazy(OP.JUMP_ABS));
							jmp.arg1 = builder.getSize() - pos + 1;
						} else {
							visitAst(n.getChildren().get(1));
							gotoLast.add(builder.addLazy(OP.JUMP_ABS));
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
				builder.add(OP.MAKE_RANGE);
				break;
			}
			case NOT: {
				visitAst(node.getChildren().get(0));
				builder.add(OP.NOT);
				break;
			}
			case IDENT:
				builder.addPool(OP.LOAD_VAR, node.getText());
				break;
			case STRING:
				builder.addPool(OP.LOAD_CONST, node.getText());
				break;
			case FOREACH: {
				assert node.getChildren().size() == 3;

				Node var = node.getChildren().get(0);
				Node container = node.getChildren().get(1);
				Node body = node.getChildren().get(2);

				visitAst(container);
				builder.add(OP.ITER_START);

				builder.addPool(OP.SET_VAR, var.getText());

				nextStack.add(new ArrayList<Code>());
				lastStack.add(new ArrayList<Code>());

				visitAst(body);

				int nextPos = builder.getSize();
				builder.add(OP.FOR_ITER);
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

				Code jumpAfterCond = builder.addLazy(OP.JUMP_IF_FALSE);
				int afterExprPos = builder.getSize();

				nextStack.add(new ArrayList<Code>());
				lastStack.add(new ArrayList<Code>());

				visitAst(body);

				builder.add(OP.JUMP, pos - builder.getSize());
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
				Code code = builder.addLazy(OP.JUMP_ABS);
				nextStack.lastElement().add(code);
				break;
			}
			case LAST: {
				Code code = builder.addLazy(OP.JUMP_ABS);
				lastStack.lastElement().add(code);
				break;
			}
			case INCLUDE: {
				visitAst(node.getChildren().get(0));
				builder.add(OP.INCLUDE);
				builder.add(OP.APPEND);
				break;
			}
			case ATTRIBUTE: {
				Node key = node.getChildren().get(0);
				Node val = node.getChildren().get(1);

				visitAst(key);
				visitAst(val);

				builder.add(OP.ATTRIBUTE);
				break;
			}
			case MAP: {
				List<Node> children = node.getChildren();
				for (int i = 0; i < children.size(); i += 2) {
					Node key = children.get(i);
					Node value = children.get(i + 1);
					if (key.getType() == NodeType.IDENT) {
						builder.addPool(OP.LOAD_CONST, key.getText());
					} else {
						visitAst(key);
					}
					visitAst(value);
				}

				builder.add(OP.MAKE_MAP, children.size() / 2);
				break;
			}
			case DOLLARVAR: {
				builder.addPool(OP.LOAD_VAR, node.getText());
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
						builder.add(OP.LC, node.getChildren().size() - 1);
					} else if (func.getText().equals("uc")) {
						if (node.getChildren().size() - 1 != 1) {
							throw new JTTCompilerError(
									"Invalid argument count for 'uc'");
						}
						builder.add(OP.UC, node.getChildren().size() - 1);
					} else if (func.getText().equals("sprintf")) {
						builder.add(OP.SPRINTF, node.getChildren().size() - 1);
					} else if (func.getText().equals("uri")
							|| func.getText().equals("url")) {
						if (node.getChildren().size() - 1 != 1) {
							throw new JTTCompilerError(
									"Invalid argument count for 'ur[il]'");
						}
						builder.add(OP.URI_ESCAPE,
								node.getChildren().size() - 1);
					} else {
						builder.addPool(OP.LOAD_CONST, func.getText());
						builder.add(OP.FUNCALL, node.getChildren().size() - 1);
					}
				} else if (func.getType() == NodeType.ATTRIBUTE) {
					visitAst(func.getChildren().get(0)); // object
					builder.addPool(OP.LOAD_CONST, func.getChildren().get(1)
							.getText()); // method name
					for (int i = 1; i < node.getChildren().size(); ++i) {
						visitAst(node.getChildren().get(i));
					}
					builder.add(OP.METHOD_CALL, node.getChildren().size() - 1);
				} else {
					throw new RuntimeException("Invalid funcall : "
							+ func.getType());
					// builder.add(OP.FUNCALL, node.getChildren().size() - 1);
				}
				break;
			}
			case WRAPPER: {
				// (template (wrapper (string foo.tt) (template (raw_string ohoho))))
				String fileName = node.getChildren().get(0).getText();
				Node body = node.getChildren().get(1);

				visitAst(body);

				builder.addPool(OP.LOAD_CONST, fileName);
				builder.add(OP.WRAP);

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
			builder.add(op);
		}

		public Irep getResult() {
			builder.add(OP.RETURN);
			return builder.build();
		}

		public void start(Node ast) throws JTTCompilerError {
			lvarStack.push(new ArrayList<>());
			this.visitAst(ast);
		}
	}

	public Irep compile(Node ast) throws ParserError, JTTCompilerError {
		if (ast == null) {
			throw new RuntimeException("nullpo");
		}
		Visitor visitor = new Visitor();
		visitor.start(ast);
		return visitor.getResult();
	}
}
