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
package org.wcs.smart.data.oda.smart.ui.impl;

import java.text.MessageFormat;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.jface.resource.ImageDescriptor;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.util.UuidUtils;

/**
 * A ODA data set designer page for an user to create a SMART ODA data set
 * design instance. This implementation allows user to select a query from the
 * query tree.
 * 
 */
public class SmartQueryAttachmentDatasetWizardPage extends SmartQueryDatasetWizardPage {

	public static final String DEFAULT_MESSAGE = Messages.CustomDataSetWizardPage_PickQuery_Message;

	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public SmartQueryAttachmentDatasetWizardPage(String pageName) {
		super(pageName);
		setMessage(DEFAULT_MESSAGE);
	}

	/**
	 * Constructor
	 * 
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public SmartQueryAttachmentDatasetWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setMessage(DEFAULT_MESSAGE);
	}


	/**
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	@Override
	protected void updateDesign(DataSetDesign dataSetDesign, IConnection conn,
			QueryEditorInput smartQuery) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(SmartQuery.SMART_ATTACHMENT_DATASET_TYPE);
		String queryText = getQuery().getType().getKey() + ":" + UuidUtils.uuidToString(getQuery().getUuid()); //$NON-NLS-1$
		query.prepare(queryText);
		dataSetDesign.setQueryText(queryText);

		try {
			IResultSetMetaData md = query.getMetaData();
			updateResultSetDesign(md, dataSetDesign);
		} catch (OdaException e) {
			// no result set definition available, reset previous derived
			// metadata
			dataSetDesign.setResultSets(null);
			e.printStackTrace();
		}

		// proceed to get parameter design definition
		try {
			IParameterMetaData paramMd = query.getParameterMetaData();
			updateParameterDesign(paramMd, dataSetDesign);
		} catch (OdaException ex) {
			// no parameter definition available, reset previous derived
			// metadata
			dataSetDesign.setParameters(null);
			ex.printStackTrace();
		}

		/*
		 * See DesignSessionUtil for more convenience methods to define a data
		 * set design instance.
		 */
		// names can not contain: / \ . ! ; , 
		//See NamePropertyType.isValidName
		String lname = smartQuery.getName();
		lname = lname.replaceAll("[/\\\\.!;,]", "_");  //$NON-NLS-1$//$NON-NLS-2$
		dataSetDesign.setDisplayName(MessageFormat.format(org.wcs.smart.data.oda.smart.internal.Messages.SmartQueryAttachmentDatasetWizardPage_AttachmentsDatasetDefaultName, lname, smartQuery.getId())); 

	}

	@Override
	protected boolean canAddQuery(QueryEditorInput in) {
		return in.getType().supportsReports() && in.getType().supportsImageResult();
	}
}
