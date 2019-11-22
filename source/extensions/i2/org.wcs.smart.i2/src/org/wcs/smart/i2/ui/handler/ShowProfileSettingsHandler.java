package org.wcs.smart.i2.ui.handler;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ui.preference.AttributesPreferencePage;
import org.wcs.smart.i2.ui.preference.EntityTypesPreferencePage;
import org.wcs.smart.i2.ui.preference.IntelPreferenceDialog;
import org.wcs.smart.i2.ui.preference.ProfilesPreferencePage;
import org.wcs.smart.i2.ui.preference.RecordsPreferencePage;
import org.wcs.smart.i2.ui.preference.RelationshipGroupsPreferencePage;
import org.wcs.smart.i2.ui.preference.RelationshipStylesPreferencePage;
import org.wcs.smart.i2.ui.preference.RelationshipTypesPreferencePage;
import org.wcs.smart.i2.ui.preference.SettingsPreferencePage;


@SuppressWarnings("restriction")
public class ShowProfileSettingsHandler { 

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, IEclipseContext context) {
		

		PreferenceManager manager = new PreferenceManager();

			
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.profiles", 
				ContextInjectionFactory.make(ProfilesPreferencePage.class, context),
				Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ROOTENTITY)));
			
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.attributes", 
				ContextInjectionFactory.make(AttributesPreferencePage.class, context),
				SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_NUMBER_ICON)));
				    
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.entitytypes", 
				ContextInjectionFactory.make(EntityTypesPreferencePage.class, context),
				Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY)));
			
		manager.addToRoot(new PreferenceNode("org.wcs.smart.i2.pref.relgroups", 
				ContextInjectionFactory.make(RelationshipGroupsPreferencePage.class, context)));

		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.relations",
				ContextInjectionFactory.make(RelationshipTypesPreferencePage.class, context),
				Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RELATIONSHIP)));
			
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.records",
				ContextInjectionFactory.make(RecordsPreferencePage.class, context),
				Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD)));
		
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.settings",
				ContextInjectionFactory.make(SettingsPreferencePage.class, context),
				Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_CONFIGURE)));
		
		manager.addToRoot(new SmartPreferenceNode("org.wcs.smart.i2.pref.styles",
				ContextInjectionFactory.make(RelationshipStylesPreferencePage.class, context),
				SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.STYLE_ICON)));
	
		
		IntelPreferenceDialog pd = new IntelPreferenceDialog(context.get(Shell.class), manager);
		ContextInjectionFactory.inject(pd, context);
		pd.open();
	}
	
	// E3
	public static class ShowProfilesSettingsHandlerWrapper extends DIHandler<ShowProfileSettingsHandler> {
		public ShowProfilesSettingsHandlerWrapper() {
			super(ShowProfileSettingsHandler.class);
		}
	}
	
	
	private class SmartPreferenceNode extends PreferenceNode {
		private ImageDescriptor imageDescriptor;
		private Image image = null;
		public SmartPreferenceNode(String id, IPreferencePage preferencePage, ImageDescriptor desc) {
			super(id, preferencePage);
			this.imageDescriptor = desc;
		}
		@Override
		public void disposeResources() {
			super.disposeResources();
		}
		@Override
		protected ImageDescriptor getImageDescriptor() {
			return imageDescriptor;
		}
	    @Override
		public Image getLabelImage() {
	        if (image == null && imageDescriptor != null) {
	            image = imageDescriptor.createImage();
	        }
	        return image;
	    }
	}
}