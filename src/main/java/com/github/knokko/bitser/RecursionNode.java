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
		String[] labels = new String[depth + 1];

		RecursionNode next = this;
		while (next != null) {
			labels[next.depth] = next.label;
			next = next.parent;
		}

		StringBuilder builder = new StringBuilder();
		int numArrows = labels.length;
		if (top == null) numArrows -= 1;
		for (String label : labels) {
			builder.append(label);
			if (numArrows-- > 0) builder.append(" -> ");
		}
		if (top != null) builder.append(top);
		return builder.toString();
	}
}
