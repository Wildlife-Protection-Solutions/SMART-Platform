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
package org.wcs.smart.cybertracker.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.cybertracker.model.ImportError.ErrorType;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Common logic for majority of {@link ICyberTrackerData} implementations.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public abstract class AbstractCyberTrackerData implements ICyberTrackerData {

	private Map<IDataMeta, List<ImportError>> problems = new HashMap<IDataMeta, List<ImportError>>();
	
	private Map<String, E> elementsMap;
	private List<S> sData;
	
	private String id;
	private Date startDate;
	private Date endDate;
	private List<Coordinate> timerTrackList;
	
	private Set<String> missingKeys = new HashSet<String>();
	
	public AbstractCyberTrackerData(Map<String, E> elementsMap, List<S> patrolData) {
		this.elementsMap = elementsMap;
		this.sData = patrolData;
	}

	public void addWarning(IDataMeta area, String problem) {
		if (!problems.containsKey(area))
			problems.put(area, new ArrayList<ImportError>());
		problems.get(area).add(new ImportError(problem, ErrorType.WARNING));
	}
	
	public void addError(IDataMeta area, String problem) {
		if (!problems.containsKey(area))
			problems.put(area, new ArrayList<ImportError>());
		problems.get(area).add(new ImportError(problem, ErrorType.ERROR));
	}
	
	public List<String> getErrors() {
		List<String> errors = new ArrayList<String>();
		for (List<ImportError> lerrors : problems.values()){
			for(ImportError err : lerrors){
				if (err.getType() == ErrorType.ERROR){
					errors.add(err.getMessage());
				}
			}
		}
		return errors;
	}
	
	public List<String> getWarnings() {
		List<String> warnings = new ArrayList<String>();
		for (List<ImportError> lerrors : problems.values()){
			for(ImportError err : lerrors){
				if (err.getType() == ErrorType.WARNING){
					warnings.add(err.getMessage());
				}
			}
		}
		return warnings;
	}
	
	public Map<IDataMeta, List<ImportError>> getProblems() {
		return problems;
	}
	
	public Map<String, E> getElementsMap() {
		return elementsMap;
	}

	public List<S> getSData() {
		if (sData == null)
			sData = new ArrayList<S>();
		return sData;
	}


	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public List<Coordinate> getTimerTrackList() {
		if (timerTrackList == null)
			timerTrackList = new ArrayList<Coordinate>();
		return timerTrackList;
	}

	public void setTimerTrackList(List<Coordinate> timerTrackList) {
		this.timerTrackList = timerTrackList;
	}
	
	public Set<String> getMissingKeys() {
		return missingKeys;
	}
	public void addMissingKey(String i) {
		missingKeys.add(i);
	}
	
}
