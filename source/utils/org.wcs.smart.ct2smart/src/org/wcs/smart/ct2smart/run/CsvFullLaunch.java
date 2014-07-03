package org.wcs.smart.ct2smart.run;

import java.io.File;
import java.sql.Connection;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.db.CsvDbLoader;

public class CsvFullLaunch {

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();

		Connection c = ConnectionUtil.getConnection();
		File file = new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test.xml");
//		File file = new File("d:\\dev\\data\\CyberTracker Data_Jan-2014\\test_short_super.xml");

		CsvDbLoader.getInstance().load(file, c);

		System.out.println("CSV DB loaded in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}

}
