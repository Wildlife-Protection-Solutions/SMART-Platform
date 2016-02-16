/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.uploader.ca;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConservationAreaInfo;

/**
 * Utilites associated with processing conservation areas
 * 
 * @author Emily
 *
 */
public class CaProcessorUtils {

	/**
	 * Updates the connect Conservation Area label 
	 * with the information from the desktop.
	 * 
	 * @param session
	 * @param info
	 * @return
	 */
	public static ConservationArea updateCaLabel(Session session, ConservationAreaInfo info){
		ConservationArea area = (ConservationArea) session.get(ConservationArea.class, 
				info.getUuid());
		
		if (area != null){
			info.setLabel( area.getName() + " [" + area.getId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return area;
	}
}
