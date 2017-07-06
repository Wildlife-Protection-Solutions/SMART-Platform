/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;

/**
 * Wrapper around any data provider that overrides the getData method
 * and returns the single (or list of) objects provided instead..
 * 
 * @author Emily
 *
 */
public class SingleItemDataProvider extends IQaDataProvider{

	private IQaDataProvider parentProvider;
	private Collection<?> objects;
	
	public SingleItemDataProvider (IQaDataProvider dataProvider, Object x){
		this.objects = Collections.singleton(x);
		this.parentProvider = dataProvider;
	}
	
	public SingleItemDataProvider (IQaDataProvider dataProvider, Collection<?> objects){
		this.objects = objects;
		this.parentProvider = dataProvider;
	}
	
	
	@Override
	public String getId() {
		return parentProvider.getId();
	}

	@Override
	public String getName(Locale l) {
		return parentProvider.getName(l);
	}

	@Override
	public Collection<?> getData(Session session,
			ConservationArea conservationArea, Date startDate, Date endDate) {
		return objects;
	}

	@Override
	public boolean supportsRoutine(IQaRoutineType type) {
		return parentProvider.supportsRoutine(type);
	}

	@Override
	public String getFeatureId(Session session, Object obj) {
		return parentProvider.getFeatureId(session, obj);
	}

	@Override
	public UUID getFeatureSource(Session session, Object obj) {
		return parentProvider.getFeatureSource(session, obj);
	}

}
