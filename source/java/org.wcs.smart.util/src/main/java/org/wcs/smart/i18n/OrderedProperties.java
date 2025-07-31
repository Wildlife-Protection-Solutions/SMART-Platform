package org.wcs.smart.i18n;

import java.util.*;

public class OrderedProperties extends Properties {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private final LinkedHashMap<Object, Object> orderedMap = new LinkedHashMap<>();

    @Override
    public synchronized Object put(Object key, Object value) {
        orderedMap.put(key, value);
        return super.put(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        orderedMap.remove(key);
        return super.remove(key);
    }

    @Override
    public synchronized void clear() {
        orderedMap.clear();
        super.clear();
    }

    @Override
    public Set<Object> keySet() {
        return orderedMap.keySet();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return orderedMap.entrySet();
    }

    @Override
    public Enumeration<Object> keys() {
        return Collections.enumeration(orderedMap.keySet());
    }

    @Override
    public Enumeration<Object> elements() {
        return Collections.enumeration(orderedMap.values());
    }

    public LinkedHashMap<Object, Object> getOrderedMap() {
        return orderedMap;
    }
}