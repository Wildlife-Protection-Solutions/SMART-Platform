package org.wcs.smart.ct2smart.run;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class InvalidXmlEncodingDetector {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BufferedReader br = null;
		try {
			String sCurrentLine;
//			String toProcess = "d:\\dev\\data\\CyberTracker Data_Jan-2014\\Okwangwo\\Okwangwo_January_2014\\wapamv3-nigeria.XML";
			String toProcess = "d:\\dev\\data\\CyberTracker Data_Jan-2014\\Mbe\\Mbe_January_2014\\wapamv3-nigeria.XML";
			br = new BufferedReader(new FileReader(toProcess));
			int x = 0;
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.contains("\u0015")) { //$NON-NLS-1$
					System.out.println(sCurrentLine);
					x = 10;
				} else if (x > 0) {
					System.out.println(sCurrentLine);
					x--;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}
