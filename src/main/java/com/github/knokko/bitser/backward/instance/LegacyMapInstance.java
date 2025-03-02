package com.github.knokko.bitser.backward.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LegacyMapInstance {

	public final HashMap<?, ?> legacyMap;
	public final Collection<Object> legacyKeys = new ArrayList<>();
	public final Collection<Object> legacyValues = new ArrayList<>();
	public Map<?, ?> newMap;

	public LegacyMapInstance(HashMap<?, ?> legacyMap) {
		this.legacyMap = legacyMap;
	}
}
