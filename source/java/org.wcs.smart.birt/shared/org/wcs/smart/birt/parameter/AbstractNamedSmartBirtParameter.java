/*
 * Copyright (C) 2020 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.birt.parameter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;


/**
 * Interface for a BIRT parameter from a SMART Named Item
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public abstract class AbstractNamedSmartBirtParameter implements ISmartBirtParameter{

	protected List<String> getValues(Class<? extends NamedItem> clazz, Session session, 
			Collection<ConservationArea> cas, Locale l) {
		if (cas.isEmpty()) return Collections.emptyList();
		
		Set<String> items = new HashSet<>();
		
		List<? extends NamedItem> stations = session.createQuery("FROM "+ clazz.getSimpleName() + " WHERE conservationArea in (:cas)", clazz) //$NON-NLS-1$ //$NON-NLS-2$
				.setParameter("cas",  cas) //$NON-NLS-1$
				.list();
		
		stations.forEach(s->items.add(s.getName()));
		List<String> sortedItems = new ArrayList<>(items);
		sortedItems.sort((a,b)->Collator.getInstance(l).compare(a, b));
		
		return sortedItems;
	}
}
