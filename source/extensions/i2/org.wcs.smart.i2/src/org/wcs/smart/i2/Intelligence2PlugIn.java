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
package org.wcs.smart.i2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.i2.birt.datasource.DesktopConnectionFactory;
import org.wcs.smart.i2.birt.datasource.IConnectionFactory;
import org.wcs.smart.i2.handlers.DeleteCaHandler;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;


/**
 * The activator class controls the plug-in life cycle
 */
public class Intelligence2PlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.i2"; //$NON-NLS-1$

	//Version of Data Structures required for current implementation
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_1; //the latest db version of tables
	
	
	public static final String ICON_ATTRIBUTE_GROUP = "org.wcs.smart.i2.icon.attribute.group"; //$NON-NLS-1$
	public static final String ICON_ATTRIBUTE_GROUP_NEW = "org.wcs.smart.i2.icon.attribute.group.new"; //$NON-NLS-1$
	public static final String ICON_ENTITY = "org.wcs.smart.i2.icon.entity"; //$NON-NLS-1$
	public static final String ICON_ENTITY_QUERY = "org.wcs.smart.i2.icon.query"; //$NON-NLS-1$
	public static final String ICON_ENTITY_QUERY_NEW = "org.wcs.smart.i2.icon.query.new"; //$NON-NLS-1$
	public static final String ICON_ENTITY_NEW = "org.wcs.smart.i2.icon.entity.new"; //$NON-NLS-1$
	public static final String ICON_RECORD = "org.wcs.smart.i2.icon.record"; //$NON-NLS-1$
	public static final String ICON_EDIT = "org.wcs.smart.i2.icon.edit"; //$NON-NLS-1$
	public static final String ICON_REFRESH = "org.wcs.smart.i2.icon.refresh"; //$NON-NLS-1$
	public static final String ICON_RECORD_NEW = "org.wcs.smart.i2.icon.record.new"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_NEW = "org.wcs.smart.i2.icon.workingset.new"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_SELECT = "org.wcs.smart.i2.icon.workingset.select"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_COPY = "org.wcs.smart.i2.icon.workingset.copy"; //$NON-NLS-1$
	public static final String ICON_PDF = "org.wcs.smart.i2.icon.print.pdf"; //$NON-NLS-1$
	public static final String ICON_RELATIONSHIP = "org.wcs.smart.i2.icon.relationship"; //$NON-NLS-1$
	public static final String ICON_SECTION_EXPAND = "org.wcs.smart.i2.icon.section.expand"; //$NON-NLS-1$
	public static final String ICON_RUN = "org.wcs.smart.i2.icon.query.run"; //$NON-NLS-1$
	public static final String ICON_EXPORT_QUERY = "org.wcs.smart.i2.icon.query.export"; //$NON-NLS-1$
	public static final String ICON_AREA = "org.wcs.smart.i2.icon.query.area"; //$NON-NLS-1$
	public static final String ICON_DELETE_SMALL = "org.wcs.smart.i2.icon.delete.small"; //$NON-NLS-1$
	public static final String ICON_CLEAR = "org.wcs.smart.i2.icon.clear"; //$NON-NLS-1$
	public static final String ICON_ENTITY_EXPORT = "org.wcs.smart.i2.icon.entity.export"; //$NON-NLS-1$
	
	public static final String ICON_LOCATION_IMPORT = "org.wcs.smart.i2.icon.location.import"; //$NON-NLS-1$
	
	public static final String ICON_IMG_ZOOMIN = "org.wcs.smart.i2.icon.zoom.in"; //$NON-NLS-1$
	public static final String ICON_IMG_ZOOMOUT = "org.wcs.smart.i2.icon.zoom.out"; //$NON-NLS-1$
	
	public static final String ICON_SRC_NEW = "org.wcs.smart.i2.icon.record.source.new"; //$NON-NLS-1$
	public static final String ICON_SRC_IP = "org.wcs.smart.i2.icon.record.source.ip"; //$NON-NLS-1$
	public static final String ICON_SRC_DONE = "org.wcs.smart.i2.icon.record.source.done"; //$NON-NLS-1$
	
	public static final String ICON_ATTACHMENT_SEARCH = "org.wcs.smart.i2.icon.attachment.search"; //$NON-NLS-1$
	
	// The shared instance
	private static Intelligence2PlugIn plugin;

	
	/**
	 * The constructor
	 */
	public Intelligence2PlugIn() {
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(ICON_ENTITY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity.png")); //$NON-NLS-1$);
		reg.put(ICON_ENTITY_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_add.png")); //$NON-NLS-1$);
		reg.put(ICON_RECORD, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/script.png")); //$NON-NLS-1$);
		reg.put(ICON_RECORD_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/script_add.png")); //$NON-NLS-1$);
		reg.put(ICON_EDIT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/edit.png")); //$NON-NLS-1$);
		reg.put(ICON_REFRESH, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/refresh.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_add.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_SELECT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_select.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_COPY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_copy.png")); //$NON-NLS-1$);
		reg.put(ICON_PDF, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/printpdf.png")); //$NON-NLS-1$);
		reg.put(ICON_RELATIONSHIP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/relationship.png")); //$NON-NLS-1$);
		reg.put(ICON_SECTION_EXPAND, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj12/section_expand.png")); //$NON-NLS-1$);
		reg.put(ICON_ATTRIBUTE_GROUP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_group.png")); //$NON-NLS-1$);
		reg.put(ICON_ATTRIBUTE_GROUP_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_group_new.png")); //$NON-NLS-1$);
		reg.put(ICON_RUN, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/run_query.png")); //$NON-NLS-1$);
		
		reg.put(ICON_ENTITY_QUERY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_query.png")); //$NON-NLS-1$);
		reg.put(ICON_ENTITY_QUERY_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/new_entity_query.png")); //$NON-NLS-1$);
		reg.put(ICON_AREA, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/area_polygon.png")); //$NON-NLS-1$);
		reg.put(ICON_DELETE_SMALL, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/delete_small.png")); //$NON-NLS-1$);
		reg.put(ICON_CLEAR, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/clear.png")); //$NON-NLS-1$);
		
		reg.put(ICON_IMG_ZOOMIN, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/zoom_in.png")); //$NON-NLS-1$);
		reg.put(ICON_IMG_ZOOMOUT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/zoom_out.png")); //$NON-NLS-1$);
		
		reg.put(ICON_EXPORT_QUERY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/query_export.png")); //$NON-NLS-1$);
		
		reg.put(ICON_SRC_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/src_new.png")); //$NON-NLS-1$);
		reg.put(ICON_SRC_IP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/src_inprogress.png")); //$NON-NLS-1$);
		reg.put(ICON_SRC_DONE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/src_done.png")); //$NON-NLS-1$);

		reg.put(ICON_ENTITY_EXPORT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_export.png")); //$NON-NLS-1$);
		
		reg.put(ICON_ATTACHMENT_SEARCH, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attachment_search.png")); //$NON-NLS-1$);
		reg.put(ICON_LOCATION_IMPORT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/point_import.png")); //$NON-NLS-1$);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	
		SmartContext.INSTANCE.setClass(IIntelligenceLabelProvider.class, new IntelligenceLabelProviderImpl());
		SmartContext.INSTANCE.setClass(IConnectionFactory.class, new DesktopConnectionFactory());
		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteCaHandler(), DeleteCaHandler.EXECUTE_ORDER);
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
	public static Intelligence2PlugIn getDefault() {
		return plugin;
	}

	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message); //$NON-NLS-1$
			}
			
		});
		
	}	
}
