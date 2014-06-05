package me.geso.jtt.parser;

import java.util.ArrayList;
import java.util.List;

public class Node {

	private String text;
	private NodeType type;
	private List<Node> children;

	public Node(NodeType type, String text) {
		this.setType(type);
		this.setText(text);
	}

	public Node(NodeType type, Node child) {
		this.setType(type);

		List<Node> children = new ArrayList<>();
		children.add(child);
		this.setChildren(children);
	}

	public Node(NodeType type, List<Node> children) {
		this.setType(type);
		this.setChildren(children);
	}

	public Node(NodeType type) {
		this.setType(type);
	}

	public Node(NodeType type, Node lhs, Node rhs) {
		this.setType(type);
		List<Node> children = new ArrayList<>();
		children.add(lhs);
		children.add(rhs);
		this.setChildren(children);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
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
