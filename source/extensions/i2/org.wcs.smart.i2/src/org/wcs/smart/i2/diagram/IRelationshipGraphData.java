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
package org.wcs.smart.i2.diagram;

import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;

/**
 * Data representation for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public interface IRelationshipGraphData {

	/**
	 * @param source
	 * @return if given entity is a root node of a graph
	 */
	public boolean isRootNode(Object entity);
	
	/**
	 * @return array of all {@link IntelEntity} to be displayed on diagram
	 */
	public IntelEntity[] getEntities();

	/**
	 * @return array of all {@link IntelEntity} that are the target for given source
	 */
	public IntelEntity[] getTargets(Object source);
	
	/**
	 * @param source
	 * @param target
	 * @return {@link IntelEntityRelationship} with given source and target. In there is no direct relationship between source and target {@code null} is returned.
	 */
	public IntelEntityRelationship getRelationship(Object source, Object target);
	
}
