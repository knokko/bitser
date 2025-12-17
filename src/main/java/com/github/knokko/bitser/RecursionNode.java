package com.github.knokko.bitser;

class RecursionNode {

	final RecursionNode parent;
	final String label;
	final int depth;

	RecursionNode(RecursionNode parent, String label) {
		this.parent = parent;
		this.label = label;
		this.depth = 1 + parent.depth;
	}

	RecursionNode(String label) {
		this.parent = null;
		this.label = label;
		this.depth = 0;
	}

	String generateTrace(String top) {
		String[] labels = new String[1 + depth];

		RecursionNode next = this;
		while (next != null) {
			labels[next.depth] = next.label;
			next = next.parent;
		}

		StringBuilder builder = new StringBuilder();
		for (String label : labels) {
			builder.append(label);
			builder.append(" -> ");
		}
		builder.append(top);
		return builder.toString();
	}
}
