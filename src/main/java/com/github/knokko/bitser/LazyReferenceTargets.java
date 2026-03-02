package com.github.knokko.bitser;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

record LazyReferenceTargets(String label, ArrayList<Object> idsToUnstable, Map<UUID, Object> stable) { }
