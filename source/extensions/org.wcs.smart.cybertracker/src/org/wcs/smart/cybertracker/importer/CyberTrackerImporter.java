/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.data.Data;
import org.wcs.smart.cybertracker.model.data.Data.Sightings;

/**
 * Importer for CyberTracker application data
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImporter {
	
	private Map<String, Data.Elements.E> elementsMap;
	private Map<String, List<Data.Sightings.S>> patrolsMap;
	
	public List<CyberTrackerPatrol> importData(File file, IProgressMonitor monitor) throws Exception {
		Data data = null;
		FileInputStream in = new FileInputStream(file);
		try {
			monitor.subTask(Messages.CyberTrackerImporter_Read_Xml);
			data = readDataModel(in);
			monitor.worked(1);
		} finally {
			in.close();
		}
		if (data == null) {
			throw new Exception(Messages.CyberTrackerImporter_Read_Error);
		}
		
		elementsMap = buildElementsMap(data);
		patrolsMap = buildPatrolsMap(data);
		
		data = null; //we don't need data object anymore
		List<CyberTrackerPatrol> patrols = new ArrayList<CyberTrackerPatrol>();
		for (String id : patrolsMap.keySet()) {
			patrols.add(new CyberTrackerPatrol(elementsMap, patrolsMap.get(id)));
		}
		return patrols;
	}

	
	private Map<String, Data.Elements.E> buildElementsMap(Data data) {
		Map<String, Data.Elements.E> result = new HashMap<String, Data.Elements.E>();
		if (data == null || data.getElements() == null)
			return result;
		for (Data.Elements.E e : data.getElements().getE()) {
			result.put(e.getI(), e);
		}
		return result;
		
	}

	/**
	 * Returns mapped patrol_id to list of {@link Sightings.S} where each sighting is related to one patrol
	 * 
	 * @param data
	 * @return mapped patrol_id to list of {@link Sightings.S}

	 */
	private Map<String, List<Data.Sightings.S>> buildPatrolsMap(Data data) {
		Map<String, List<Data.Sightings.S>> result = new HashMap<String, List<Data.Sightings.S>>();
		if (data == null || data.getSightings() == null)
			return result;
		for (Data.Sightings.S s : data.getSightings().getS()) {
			//fetch patrol id value
			String patrolId = null;
			for (Data.Sightings.S.A a : s.getA()) {
				if (PatrolScreensUtil.RESULT_PATROL_ID.equals(a.getN())) {
					patrolId = a.getV();
				}
			}
			if (patrolId == null)
				continue;
			
			List<Data.Sightings.S> lst = result.get(patrolId);
			if (lst == null) {
				lst = new ArrayList<Data.Sightings.S>();
				result.put(patrolId, lst);
			}
			lst.add(s);
		}
		return result;
	}
	
	/**
	 * Reads data data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read data from
	 * @return
	 * @throws JAXBException
	 */
	public static Data readDataModel(InputStream file) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Data.class);
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(file);
		return (Data) o;
	}
	
}
