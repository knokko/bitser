package com.github.knokko.bitser;

class PostInitJob {

	final BitPostInit structObject;
	final BitPostInit.Context context;

	PostInitJob(BitPostInit structObject, BitPostInit.Context context) {
		this.structObject = structObject;
		this.context = context;
	}
}
