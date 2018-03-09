/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;

import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;

/**
 * Shared query manager
 * 
 * @author Emily
 *
 */
public enum QueryManager {
	INSTANCE;
	
	public AbstractIntelQuery findQuery(Session session, UUID uuid, String typeKey) {
		if (typeKey.equals(IntelRecordObservationQuery.KEY)) {
			return session.get(IntelRecordObservationQuery.class, uuid);
		}else if (typeKey.equals(IntelEntitySummaryQuery.KEY)) {
			return session.get(IntelEntitySummaryQuery.class, uuid);
		}
		return null;
	}
	
	public AbstractIntelQuery findQuery(Session session, UUID uuid) {
		AbstractIntelQuery q = null;
		q = session.get(IntelRecordObservationQuery.class, uuid);
		if (q != null) return q;
		q = session.get(IntelEntitySummaryQuery.class, uuid);
		return q;
	}
}
