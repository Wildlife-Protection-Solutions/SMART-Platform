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
package org.wcs.smart.er;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.internal.SurveyDeleteCaHandler;
/**
 * The activator class controls the plug-in life cycle
 */
public class EcologicalRecordsPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.er"; //$NON-NLS-1$

	//The current db version
	public static final String DB_VERSION = "1.0"; //$NON-NLS-1$
	
	// The shared instance
	private static EcologicalRecordsPlugIn plugin;
	
	public static final String SURVEY_DESIGN_ICON = "org.wcs.smart.surveydesign.icon"; //$NON-NLS-1$
	public static final String SURVEY_DESIGN_INACTIVE_ICON = "org.wcs.smart.surveydesign.inactive.icon"; //$NON-NLS-1$
	public static final String MISSION_ICON = "org.wcs.smart.fieldsurvey.icon"; //$NON-NLS-1$
	public static final String SURVEY_ICON = "org.wcs.smart.survey.icon"; //$NON-NLS-1$
	public static final String NEW_MISSION_ICON = "org.wcs.smart.fieldsurvey.new.icon"; //$NON-NLS-1$
	public static final String NEW_SURVEY_ICON = "org.wcs.smart.survey.new.icon"; //$NON-NLS-1$
	public static final String SAMPLING_UNIT_ICON = "org.wcs.smart.survey.samplingunit"; //$NON-NLS-1$
	public static final String SAMPLING_UNIT_ATTRIBUTE_ICON = "org.wcs.smart.survey.samplingunitattribute"; //$NON-NLS-1$
	public static final String SAMPLING_UNIT_PLOT_ICON = "org.wcs.smart.survey.samplingunit.plot"; //$NON-NLS-1$
	public static final String SAMPLING_UNIT_TRANSECT_ICON = "org.wcs.smart.survey.samplingunit.transect"; //$NON-NLS-1$
	public static final String SAMPLING_UNIT_RECON_ICON = "org.wcs.smart.survey.samplingunit.recon"; //$NON-NLS-1$
	
	public static final String DELETE_ICON = "org.wcs.smart.er.delete"; //$NON-NLS-1$
	public static final String CLEAR_SELECTION_ICON = "org.wcs.smart.er.clearselection"; //$NON-NLS-1$
	public static final String CHANGE_STATE_ICON = "org.wcs.smart.er.su.state"; //$NON-NLS-1$
	public static final String EDIT_SU_ICON = "org.wcs.smart.er.su.edit"; //$NON-NLS-1$
	public static final String ZOOM_SU_ICON = "org.wcs.smart.er.su.zoom"; //$NON-NLS-1$
	public static final String SUEXPORT_ICON = "org.wcs.smart.er.su.export"; //$NON-NLS-1$
	public static final String SUIMPORT_ICON = "org.wcs.smart.er.su.import"; //$NON-NLS-1$
	
	public static final String IMPORT_TRACK_ICON = "org.wcs.smart.er.track.import"; //$NON-NLS-1$
	public static final String SPLIT_TRACK_ICON = "org.wcs.smart.er.track.split"; //$NON-NLS-1$
	public static final String MERGE_TRACK_ICON = "org.wcs.smart.er.track.merge"; //$NON-NLS-1$
	public static final String EDIT_TRACK_ICON = "org.wcs.smart.er.track.edit"; //$NON-NLS-1$
	
	public static final String MISSION_LEADER_ICON = "org.wcs.smart.er.mission.leader"; //$NON-NLS-1$
	public static final String MISSION_MEMBER_ICON = "org.wcs.smart.er.mission.member"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public EcologicalRecordsPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SurveyDeleteCaHandler deleteCaHandler = new SurveyDeleteCaHandler();
		ConservationAreaManager.getInstance().addDeleteHandler(deleteCaHandler,SurveyDeleteCaHandler.EXECUTE_ORDER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EcologicalRecordsPlugIn getDefault() {
		return plugin;
	}

    public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
    
    protected void initializeImageRegistry(ImageRegistry reg) {
     	super.initializeImageRegistry(reg);
     	reg.put(SURVEY_DESIGN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_design.png")); //$NON-NLS-1$
     	reg.put(SURVEY_DESIGN_INACTIVE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_design_inactive.png")); //$NON-NLS-1$
     	reg.put(SURVEY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey.png")); //$NON-NLS-1$
     	reg.put(MISSION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/mission.png")); //$NON-NLS-1$
     	reg.put(SAMPLING_UNIT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit.png")); //$NON-NLS-1$
     	reg.put(SAMPLING_UNIT_ATTRIBUTE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_attribute.png")); //$NON-NLS-1$
     	reg.put(SAMPLING_UNIT_PLOT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_plot.png")); //$NON-NLS-1$
     	reg.put(SAMPLING_UNIT_TRANSECT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_transect.png")); //$NON-NLS-1$
     	reg.put(SAMPLING_UNIT_RECON_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_recon.png")); //$NON-NLS-1$
     	
     	reg.put(DELETE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/delete.png")); //$NON-NLS-1$
     	reg.put(CLEAR_SELECTION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/clear_selection.png")); //$NON-NLS-1$
     	reg.put(CHANGE_STATE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/change_state.png")); //$NON-NLS-1$
     	reg.put(EDIT_SU_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_edit.png")); //$NON-NLS-1$
     	reg.put(ZOOM_SU_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_zoom.png")); //$NON-NLS-1$
     	
     	reg.put(SUEXPORT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_export.png")); //$NON-NLS-1$
     	reg.put(SUIMPORT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sampling_unit_import.png")); //$NON-NLS-1$
     	
     	reg.put(NEW_MISSION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/mission_add.png")); //$NON-NLS-1$
     	reg.put(NEW_SURVEY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_add.png")); //$NON-NLS-1$
     	
     	reg.put(MISSION_LEADER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/mission_leader.png")); //$NON-NLS-1$
     	reg.put(MISSION_MEMBER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/mission_member.png")); //$NON-NLS-1$
    
     	reg.put(IMPORT_TRACK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/import_track.png")); //$NON-NLS-1$
     	reg.put(SPLIT_TRACK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/split_track.png")); //$NON-NLS-1$
     	reg.put(EDIT_TRACK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/edit_track.png")); //$NON-NLS-1$
     	reg.put(MERGE_TRACK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/merge_track.png")); //$NON-NLS-1$
    }
    
    
	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EcologicalRecordsPlugIn_ErrorDialogTitle, message);
			}
			
		});
		
	}
}
