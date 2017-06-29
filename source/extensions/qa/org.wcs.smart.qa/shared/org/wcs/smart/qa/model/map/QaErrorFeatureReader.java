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
package org.wcs.smart.qa.model.map;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.qa.model.QaError;

/**
 * Feature reader for iterating through a collection of QaErrors
 * and converting them to features.
 * 
 * @author Emily
 *
 */
public class QaErrorFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;	
	private QaError nextError = null;
	private Iterator<QaError> iterator;
	
	public QaErrorFeatureReader(Collection<QaError> errors, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.iterator = errors.iterator();
		
		moveNext();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	private void moveNext(){
		while(iterator.hasNext()){
			QaError next = iterator.next();
			if (QaErrorMemoryDatastore.isValid(next, ftype)){
				nextError = next;
				return;
			}
		}
		nextError = null;
	}
	
	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		return nextError != null;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {		
		SimpleFeature sf =  FeatureFactory.createQaFeature(ftype, nextError, Locale.getDefault());
		moveNext();
		return sf;
	}
}