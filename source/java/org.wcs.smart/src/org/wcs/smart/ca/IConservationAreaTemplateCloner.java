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
package org.wcs.smart.ca;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Interceptor;
/**
 * Interface to implement for copying conservation area
 * information when creating a new conservation area from
 * a template.
 * 
 * @author Emily
 *
 */
public interface IConservationAreaTemplateCloner {

	
	/**
	 * Clones template based information from the templateCa into the
	 * newCa.
	 * 
	 * @param engine 
	 * @param monitor  
	 * 
	 */
	void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception;

	/**
	 * Any interceptors that need to be added to hibernate session
	 * @return
	 */
	default Collection<? extends Interceptor> getInterceptors() {
		return Collections.emptyList();
	}


}
