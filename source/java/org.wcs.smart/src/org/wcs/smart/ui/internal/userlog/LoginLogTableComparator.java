package org.wcs.smart.ui.internal.userlog;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.wcs.smart.LoginLogEntry;

public class LoginLogTableComparator extends ViewerComparator {
    private int propertyIndex;
    private static final int DESCENDING = 1;
    private int direction = DESCENDING;

    public LoginLogTableComparator() {
        this.propertyIndex = 0;
        direction = DESCENDING;
    }

    public int getDirection() {
        return direction == 1 ? SWT.DOWN : SWT.UP;
    }

    public void setColumn(int column) {
        if (column == this.propertyIndex) {
            // Same column as last sort; toggle the direction
            direction = 1 - direction;
        } else {
            // New column; do an ascending sort
            this.propertyIndex = column;
            direction = DESCENDING;
        }
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        LoginLogEntry p1 = (LoginLogEntry) e1;
        LoginLogEntry p2 = (LoginLogEntry) e2;
        int rc = 0;
        switch (propertyIndex) {
        case 0:
            rc = p1.getCaName().compareTo(p2.getCaName());
            break;
        case 1:
            rc = p1.getSmartUserId().compareTo(p2.getSmartUserId());
            break;
        case 2:
            rc = p1.getUserLevels().compareTo(p2.getUserLevels());
            break;
        case 3:
            rc = p1.getLoginTimestamp().compareTo(p2.getLoginTimestamp());
            break;
        default:
            rc = 0;
        }
        // If descending order, flip the direction
        if (direction == DESCENDING) {
            rc = -rc;
        }
        return rc;
    }

}

