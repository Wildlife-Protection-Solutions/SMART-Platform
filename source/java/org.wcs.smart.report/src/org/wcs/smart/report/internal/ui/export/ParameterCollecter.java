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
package org.wcs.smart.report.internal.ui.export;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IParameterDefnBase;
import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.birt.report.engine.api.impl.ScalarParameterDefn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.execute.ParameterFinder;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.viewer.parameter.BooleanParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.DateParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.GroupedReportParameters;
import org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.NumberParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.ReportParameterDialog;
import org.wcs.smart.report.internal.ui.viewer.parameter.SmartDateParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.StringParameterComponent;
import org.wcs.smart.report.model.Report;

/**
 * Parameter collector that prompts the user to enter all parameters
 * required for a collection of reports.
 * <p>If two reports have the same parameter name but different parameter
 * type an exception is thrown. Each parameter name must have a unique type.
 * </p>
 * <p>
 * Only one value can be provided for each parameter name.
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class ParameterCollecter {

	private static final String PARAMTYPE_NOTSUPPORTED_MSG = Messages.ParameterCollecter_TypeNotSupported;
	private HashMap<String, IParameterDefnBase> allParameters = null;
	private HashMap<String, Object> paramValues = null;
	
	/**
	 * Gets all parameters associated with the given reports.
	 * @param reports reports to collect parameters for
	 * @return a map of parameter name to parameter value or null if cancelled
	 * @throws Exception
	 */
	public  HashMap<String, Object> getParameters(Report[] reports) throws Exception{
		allParameters = new HashMap<String, IParameterDefnBase>();
		for (int i = 0; i < reports.length; i++){
			allParameters.putAll( ParameterFinder.INSTANCE.getParameters(reports[i], ReportEngineManager.getBirtReportEngine()));
		}
		displayParameters();
		return paramValues;
	}
	
	private IBirtParameterComponent getComponentForParameter(IParameterDefn ptype) throws Exception{
		Object defaultValue = null;
		if (ptype instanceof ScalarParameterDefn){
			defaultValue = ((ScalarParameterDefn)ptype).getDefaultValue(); 
		}
		
		if (ptype.getDataType() == IParameterDefn.TYPE_DATE || 
				ptype.getDataType() == IParameterDefn.TYPE_TIME ||
				ptype.getDataType() == IParameterDefn.TYPE_DATE_TIME) {
			boolean date = ptype.getDataType() == IParameterDefn.TYPE_DATE  || ptype.getDataType() == IParameterDefn.TYPE_DATE_TIME;
			boolean time = ptype.getDataType() == IParameterDefn.TYPE_TIME  || ptype.getDataType() == IParameterDefn.TYPE_DATE_TIME;
			return new DateParameterComponent(ptype.getName(), ptype.getPromptText(), date, time, defaultValue);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_DECIMAL){
			return new NumberParameterComponent(ptype.getName(), ptype.getPromptText(), NumberParameterComponent.DOUBLE_VALIDATOR, defaultValue);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_INTEGER){
			return new NumberParameterComponent(ptype.getName(), ptype.getPromptText(), NumberParameterComponent.INTEGER_VALIDATOR, defaultValue);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_FLOAT){
			return new NumberParameterComponent(ptype.getName(), ptype.getPromptText(), NumberParameterComponent.FLOAT_VALIDATOR, defaultValue);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_STRING){
			return new StringParameterComponent(ptype.getName(), ptype.getPromptText(), defaultValue);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_BOOLEAN){
			return new BooleanParameterComponent(ptype.getName(), ptype.getPromptText(), defaultValue);
		}else{
			throw new Exception(MessageFormat.format(
					Messages.ParameterCollecter_TypeXNotSupported, new Object[]{ ptype.getDataType() }));
		}		
	}
	/*
	 * Displays a parameter dialog to the user where the user can enter
	 * values.
	 */
	private void displayParameters( ) throws Exception{
		if (allParameters.size() == 0){
			//no parameters to get for this report
			paramValues = new HashMap<String, Object>();
			return;
		}
		final ReportParameterDialog dialog = new ReportParameterDialog(Display.getDefault().getActiveShell());
		for (Iterator<IParameterDefnBase> iterator = allParameters.values().iterator(); iterator.hasNext();) {
			IParameterDefnBase paramPart = (IParameterDefnBase)iterator.next();
			
			if (paramPart instanceof IParameterDefn){
				IBirtParameterComponent part = getComponentForParameter((IParameterDefn) paramPart);
				if (part != null){
					dialog.addComponent(part);
				}
			}else if(paramPart instanceof IParameterGroupDefn){
				IParameterGroupDefn def = (IParameterGroupDefn)paramPart;
				GroupedReportParameters groupedComponent = new GroupedReportParameters(def);
				
				if (def.getName().equals(SmartReportParameters.PARAM_DATEGROUP_NAME)){
					SmartDateParameterComponent part = new SmartDateParameterComponent(def);
					groupedComponent.addComponent(part);
				}else{
					ArrayList<?> parts = def.getContents();
					for (Object object : parts) {
						if (object instanceof IParameterDefn){
							IBirtParameterComponent part = getComponentForParameter((IParameterDefn) object);
							if (part != null){
								groupedComponent.addComponent(part);
							}
						}else{
							throw new Exception(MessageFormat.format(
									PARAMTYPE_NOTSUPPORTED_MSG, new Object[]{paramPart.getClass().getName()}));		
						}
					}
				}
				dialog.addComponent(groupedComponent);
				
			}else{
				throw new Exception(MessageFormat.format(
						PARAMTYPE_NOTSUPPORTED_MSG, new Object[]{paramPart.getClass().getName()}));
			}
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				paramValues = null;
				if (dialog.open() == Window.OK) {
					paramValues = dialog.getValues();
				}

			}
		});
	}
	
}
