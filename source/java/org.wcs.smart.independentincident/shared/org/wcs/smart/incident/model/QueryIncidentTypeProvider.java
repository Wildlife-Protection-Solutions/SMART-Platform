/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.observation.query.model.IIncidentTypeProvider;
import org.wcs.smart.observation.query.model.QueryIncidentType;

/**
 * Incident type provider for querying incident types provided
 * by this plugin.
 */
public class QueryIncidentTypeProvider implements IIncidentTypeProvider {

	public QueryIncidentTypeProvider() {
	}

	@Override
	public Collection<QueryIncidentType> getTypes(Session session, Collection<ConservationArea> cas) {
		
		Map<String, QueryIncidentType> qtypes = new HashMap<>();
		
		List<IncidentType> types = session.createQuery("FROM IncidentType WHERE conservationArea in (:cas)", IncidentType.class) //$NON-NLS-1$
				.setParameterList("cas",cas) //$NON-NLS-1$
				.list();
		for (IncidentType type : types) {
			if (qtypes.containsKey(type.getKeyId())) {
				qtypes.get(type.getKeyId()).getUuids().add(type.getUuid());
				if (!qtypes.get(type.getKeyId()).isActive() && type.getIsActive()) {
					qtypes.get(type.getKeyId()).setIsActive(true);
				}
			}else {
				QueryIncidentType ntype = new QueryIncidentType(type.getName(), type.getKeyId(), type.getUuid(), type.getIsActive());
				qtypes.put(type.getKeyId(), ntype);
			}
		}
		
		return qtypes.values();
	}

}
