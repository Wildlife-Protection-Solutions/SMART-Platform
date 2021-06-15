/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt.attachments;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.incident.birt.AbstractIncidentDataset;
import org.wcs.smart.incident.birt.SmartIncidentConnection;

/**
 * Incident attachments dataset
 * 
 * @author Emily
 *
 */
public class IncidentAttachmentDataset extends AbstractIncidentDataset{

	public static final String DATASET_TYPE = "org.wcs.smart.incident.birt.incident.attachments"; //$NON-NLS-1$
	
	public IncidentAttachmentDataset(SmartIncidentConnection connection) {
		super(connection, DATASET_TYPE);
	}
	
	@Override
	public void prepare(String queryText) throws OdaException {
	}

	@Override
	protected IResultSetMetaData createMetadata() {
		return new IncidentAttachmentDatasetResultSetMetadata();
	}

	@Override
	public IResultSet executeQuery() throws OdaException {
		return new IncidentAttachmentDatasetResultSet(getIncidentUuid(), 
				(IncidentAttachmentDatasetResultSetMetadata)getMetaData(), connection);
	}
	
}