
package org.wcs.smart.event.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.observation.model.WaypointObservation;

public class NotifyUserActionType implements IActionType {

	
	public static IActionParameter STRING_PARAMETER = new GeneralItemAction("string1", "String Parameter", true);
	public static IActionParameter NUMBER_PARAMETER = new GeneralItemAction("number1", "NumberParameter", true);
	
	List<IActionParameter> parameters = new ArrayList<>();

	public NotifyUserActionType() {
		parameters.add(STRING_PARAMETER);
		parameters.add(NUMBER_PARAMETER);
	}

	@Override
	public String getKey() {
		return "org.wcs.smart.event.actiontype.notify";
	}

	@Override
	public String getName(Locale l) {
		return "Notify User";
	}

	@Override
	public String getDescription(Locale l) {
		return "prints the action to the console Less than two months before Google I/O, Google has rebranded its Android Wear watch platform to \"Wear OS.\" The recent name change is part of a move to have its watches stand apart from Android, but it could also indicate that Google's smartwatch strategy is about to shift. Google may release a completely new Wear OS focused on the Google Assistant ";
	}
	
	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void performAction(EAction action, EFilter filter, WaypointObservation data) {
		System.out.println("PERFORMATION ACTION: " + getKey() + " : " + getName(Locale.getDefault()));
		System.out.println(data.getCategory().getHkey().toString());

	}

	private static class GeneralItemAction implements IActionParameter{

		public String key;
		public String name;
		public boolean isrequired;
		
		public GeneralItemAction(String key, String name, boolean required) {
			this.key = key;
			this.name = name;
			this.isrequired = required;
		}
		
		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getName(Locale l) {
			return name;
		}

		@Override
		public boolean isRequired() {
			return isrequired;
		}
		
	}
}
