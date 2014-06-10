package me.geso.jtt.parser;

import java.util.ArrayList;
import java.util.List;

public class Node {

	private final String text;
	private final NodeType type;
	private final List<Node> children;

	public Node(NodeType type) {
		this.type = type;
		this.text = null;
		this.children = null;
	}

	public Node(NodeType type, String text) {
		this.type = type;
		this.text = text;
		this.children = null;
	}

	public Node(NodeType type, Node child) {
		this.type = type;
		this.text = null;

		List<Node> children = new ArrayList<>();
		children.add(child);
		this.children = children;
	}

	public Node(NodeType type, List<Node> children) {
		this.type = type;
		this.text = null;
		this.children = children;
	}

	public Node(NodeType type, Node lhs, Node rhs) {
		this.type = type;
		this.text = null;

		List<Node> children = new ArrayList<>();
		children.add(lhs);
		children.add(rhs);
		this.children = children;
	}

	public String getText() {
		return text;
	}

	public List<Node> getChildren() {
		return children;
	}

	public NodeType getType() {
		return type;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		builder.append(type.toString().toLowerCase());
		if (children != null) {
			builder.append(' ');
			for (int i = 0; i < children.size(); i++) {
				builder.append(children.get(i));
				if (i != children.size() - 1) {
					builder.append(' ');
				}
			}
		}
		if (text != null) {
			builder.append(' ');
			builder.append(text);
		}
		builder.append(')');
		return builder.toString();
	}
}
