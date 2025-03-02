package com.github.knokko.bitser.backward.instance;

import java.util.HashMap;
import java.util.Map;

public class LegacyMapInstance {

	public final HashMap<?, ?> legacyMap;
	public Map<?, ?> newMap;

	public LegacyMapInstance(HashMap<?, ?> legacyMap) {
		this.legacyMap = legacyMap;
	}
}
