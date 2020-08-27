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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IParameterDefnBase;
import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.birt.report.engine.api.IScalarParameterDefn;
import org.eclipse.birt.report.engine.api.impl.ParameterSelectionChoice;
import org.eclipse.birt.report.engine.api.impl.ScalarParameterDefn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.execute.ParameterFinder;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.viewer.parameter.BooleanParameterComponent;
import org.wcs.smart.report.internal.ui.viewer.parameter.ComboParameterComponent;
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
	
	private List<IParameterDefnBase> allParameters = null;
	private Map<Report, Map<String, Object>> paramValues = null;
	private Map<Report, List<IParameterDefnBase>> reportParameters;
	/**
	 * Gets all parameters associated with the given reports.
	 * @param reports reports to collect parameters for
	 * @return a map of parameter name to parameter value or null if cancelled
	 * @throws Exception
	 */
	public  Map<Report, Map<String, Object>> getParameters(Report[] reports) throws Exception{
		allParameters = new ArrayList<>();
		reportParameters = new HashMap<>();
		
		for (int i = 0; i < reports.length; i++){
			List<IParameterDefnBase> all = ParameterFinder.INSTANCE.getParameters(reports[i], ReportEngineManager.getBirtReportEngine());
			
			List<IParameterDefnBase> thisparameters = new ArrayList<>();
			
			for (IParameterDefnBase newp : all) {
				IParameterDefnBase existing = null;
				for (IParameterDefnBase curr : allParameters) {
					if (isSame(curr, newp)){
						existing = curr;
						break;
					}
				}
				if (existing == null) existing = newp;
				thisparameters.add(existing);
			}
			
			reportParameters.put(reports[i], thisparameters);
			for (IParameterDefnBase d : thisparameters) {
				if (!allParameters.contains(d)) allParameters.add(d);
			}
			//allParameters.putAll( ParameterFinder.INSTANCE.getParameters(reports[i], ReportEngineManager.getBirtReportEngine()));
		}
		displayParameters();
		return paramValues;
	}
	private boolean isSame(IParameterDefnBase a, IParameterDefnBase b) {
		if (!a.getClass().equals(b.getClass())) return false;
		if (!a.getName().equals(b.getName())) return false;
		if (a.getParameterType() != b.getParameterType()) return false;
		if (a instanceof IParameterDefn && b instanceof IParameterDefn) {
			IParameterDefn aa = (IParameterDefn)a;
			IParameterDefn bb = (IParameterDefn)b;
			
			if (aa.getDataType() != bb.getDataType()) return false;
			if (aa.getSelectionListType() != bb.getSelectionListType()) return false;
			if (aa.isHidden() != bb.isHidden()) return false;
			if (aa.isRequired() != bb.isRequired()) return false;
			
			if (aa.getSelectionList().size() != bb.getSelectionList().size()) return false;
			
			
			for (Object ai : aa.getSelectionList()) {
				if (!(ai instanceof ParameterSelectionChoice)) return false;
				
				for (Object bi  : bb.getSelectionList()) {
					if (!(bi instanceof ParameterSelectionChoice)) return false;
					if (!((ParameterSelectionChoice)bi).getValue().equals(  ((ParameterSelectionChoice)ai).getValue())) return false;
				}
			}
		}
		if (a instanceof IScalarParameterDefn && b instanceof IScalarParameterDefn) {
			IScalarParameterDefn sa = (IScalarParameterDefn)a;
			IScalarParameterDefn sb = (IScalarParameterDefn)b;
			if (sa.getControlType() != sb.getControlType()) return false;
			if (!sa.getHandle().getCustomXml().equals(sb.getHandle().getCustomXml())) return false;
		}
		return true;
		
	}
	private IBirtParameterComponent getComponentForParameter(IParameterDefn ptype) throws Exception{

		if (ptype instanceof ScalarParameterDefn && 
				((ScalarParameterDefn)ptype).getControlType() == IScalarParameterDefn.LIST_BOX) {
			return new ComboParameterComponent(ptype);
		}
			
		if (ptype.getDataType() == IParameterDefn.TYPE_DATE || 
				ptype.getDataType() == IParameterDefn.TYPE_TIME ||
				ptype.getDataType() == IParameterDefn.TYPE_DATE_TIME) {
			return new DateParameterComponent(ptype);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_DECIMAL){
			return new NumberParameterComponent(ptype, NumberParameterComponent.DOUBLE_VALIDATOR);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_INTEGER){
			return new NumberParameterComponent(ptype, NumberParameterComponent.INTEGER_VALIDATOR);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_FLOAT){
			return new NumberParameterComponent(ptype, NumberParameterComponent.FLOAT_VALIDATOR);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_STRING){
			return new StringParameterComponent(ptype);
		}else if (ptype.getDataType() == IParameterDefn.TYPE_BOOLEAN){
			return new BooleanParameterComponent(ptype);
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
			paramValues = new HashMap<Report, Map<String,Object>>();
			return;
		}
		final ReportParameterDialog dialog = new ReportParameterDialog(Display.getDefault().getActiveShell());
		
		List<IBirtParameterComponent> others = new ArrayList<>();
		
		for (IParameterDefnBase paramPart : allParameters) {
			if (paramPart instanceof IParameterDefn){
				IBirtParameterComponent part = getComponentForParameter((IParameterDefn) paramPart);
				if (part != null) others.add(part);
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
		
		//other group
		if (!others.isEmpty()) {
			GroupedReportParameters groupedComponent = new GroupedReportParameters(Messages.ParameterCollecter_OtherParamGroup);
			for (IBirtParameterComponent other : others) {
				groupedComponent.addComponent(other);
			}
			dialog.addComponent(groupedComponent);
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				paramValues = null;
				if (dialog.open() == Window.OK) {
					paramValues = new HashMap<>();
					
					Map<IParameterDefn,Object> values= dialog.getValues();
					
					for (Entry<Report, List<IParameterDefnBase>> e : reportParameters.entrySet()) {
						Map<String,Object> pvalues = new HashMap<>();
						for (IParameterDefnBase pb : e.getValue()) {
							if (pb instanceof IParameterGroupDefn) {
								for (Object x : ((IParameterGroupDefn)pb).getContents()) {
									if (x instanceof IParameterDefn) {
										pvalues.put(((IParameterDefnBase) x).getName(), values.get(x));

									}
								}
							}else {
								pvalues.put(pb.getName(), values.get(pb));
							}
						}
						paramValues.put(e.getKey(), pvalues);
					}
				}

			}
		});
	}
	
}
