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
package org.wcs.smart.query.internal.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.wcs.smart.query.model.Query.QueryType;

/**
 * Handler for creating a new query that
 * prompts the user for the type of query they want o created.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreateQueryHandler extends CreateHandler {

	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		super.execute(event);
		
		QueryType qType = QueryType.OBSERVATION;
		if (event.getCommand().getId().equals("org.wcs.smart.query.createPatrolQuery")){ //$NON-NLS-1$
			qType = QueryType.PATROL;
		}else if (event.getCommand().getId().equals("org.wcs.smart.query.createSummary")){ //$NON-NLS-1$
			qType = QueryType.SUMMARY;
		}else if (event.getCommand().getId().equals("org.wcs.smart.query.createGriddedSummary")){ //$NON-NLS-1$
			qType = QueryType.GRIDDED;
		}else if (event.getCommand().getId().equals("org.wcs.smart.query.createWaypointQuery")){ //$NON-NLS-1$
			qType = QueryType.WAYPOINT;
		}else if (event.getCommand().getId().equals("org.wcs.smart.query.createQuery")){ //$NON-NLS-1$
			qType = QueryType.OBSERVATION;
		}
		super.createQuery(qType);
		return null;
	}

}
