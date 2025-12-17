package com.github.knokko.bitser;

class PostInitJob {

	final BitPostInit structObject;
	final BitPostInit.Context context;
	final RecursionNode node;

	PostInitJob(BitPostInit structObject, BitPostInit.Context context, RecursionNode node) {
		this.structObject = structObject;
		this.context = context;
		this.node = node;
	}
}
