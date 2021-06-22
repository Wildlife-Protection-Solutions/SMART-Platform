package org.wcs.smart.i2.migrate;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.i2.security.IntelAdminUserLevel;
import org.wcs.smart.ui.UserNamePasswordDialog;

public enum UserValidationManager {

	INSTANCE;

	/**
	 * return null if users cannot be validated
	 * @param toValidate
	 * @param smart6
	 * @param shell
	 * @param task
	 * @return
	 */
	public Map<ConservationArea, Employee> validate(Collection<ConservationArea> toValidate, Smart6Database smart6, Shell shell,
			SubMonitor task) {

		HashMap<ConservationArea, Employee> employeeMap = new HashMap<>();
		
		task.split(1).beginTask(Messages.ValidateUserJob_smart6subtask, 1);

		UserNamePasswordDialog udialog = new UserNamePasswordDialog(shell, Messages.ValidateUserJob_DialogTitle,
				Messages.ValidateUserJob_AllCaMsg, IDialogConstants.OK_LABEL);
		if (udialog.open() != Window.OK) {
			return null;
		}

		String username = udialog.getUserName();
		String password = udialog.getPassword();

		List<ConservationArea> smart6error = new ArrayList<>();
		for (ConservationArea ca : toValidate) {
			try {
				if (!smart6.validateUser(ca, username, password)) {
					smart6error.add(ca);
				}
			} catch (SQLException e) {
				MigratePlugin.log(e.getMessage(), e);
				smart6error.add(ca);
			}
		}
		while (!smart6error.isEmpty()) {
			ConservationArea ca = smart6error.remove(0);
			udialog = new UserNamePasswordDialog(shell, Messages.ValidateUserJob_DialogTitle,
					MessageFormat.format(Messages.ValidateUserJob_Invalid6Ca, ca.getNameLabel()),
					IDialogConstants.OK_LABEL);

			if (udialog.open() != Window.OK) {
				return null;
			}
			try {
				if (!smart6.validateUser(ca, udialog.getUserName(), udialog.getPassword())) {
					smart6error.add(0, ca);
				}
			} catch (SQLException e) {
				MigratePlugin.log(e.getMessage(), e);
				smart6error.add(0, ca);
			}
		}

		// valid smart7
		task.split(1).beginTask(Messages.ValidateUserJob_smart7subtask, 1);
		List<ConservationArea> smart7error = new ArrayList<>();
		for (ConservationArea ca : toValidate) {
			Employee emp = HibernateManager.validateUser(username, password, ca);
			if (emp == null || !emp.getSmartUserLevels().contains(IntelAdminUserLevel.INSTANCE.getKey())) {
				smart7error.add(0, ca);
			} else {
				employeeMap.put(ca, emp);
			}
		}
		while (!smart7error.isEmpty()) {
			ConservationArea ca = smart7error.remove(0);
			udialog = new UserNamePasswordDialog(shell, Messages.ValidateUserJob_DialogTitle,
					MessageFormat.format(Messages.ValidateUserJob_Invalid7Ca, ca.getNameLabel(),
							IntelAdminUserLevel.INSTANCE.getKey()),
					IDialogConstants.OK_LABEL);

			if (udialog.open() != Window.OK) {
				return null;
			}
			Employee emp = HibernateManager.validateUser(udialog.getUserName(), udialog.getPassword(), ca);
			if (emp == null || !emp.getSmartUserLevels().contains(IntelAdminUserLevel.INSTANCE.getKey())) {
				smart7error.add(0, ca);
			} else {
				employeeMap.put(ca, emp);
			}
		}
		return employeeMap;

	}
}
