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
package org.wcs.smart.intelligence.informant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for saving/loading data to/from file.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class PersistentManager {

	public static boolean toFile(File file, Object obj) {
		if (file == null) {
			return true;
		}
		if (!file.exists()) {
			SmartUtils.createDirectory(file.getParentFile());
			try {
				file.createNewFile();
			} catch (final IOException e) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						SmartPlugIn.displayLog(
								Messages.PersistentManager_FileCreateError,
								e);
					}});
				return false;
			}
		}
		try {
			OutputStream fout = new FileOutputStream(file);
			OutputStream buffer = new BufferedOutputStream(fout);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(obj);
				return true;
			} finally {
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		return false;
	}

	public static Object fromFile(File file) {
		try {
			InputStream fout = new FileInputStream(file);
			InputStream buffer = new BufferedInputStream(fout);
			ObjectInput input = new ObjectInputStream (buffer);
			try {
				return input.readObject();
			} finally {
				input.close();
			}
		} catch (IOException e) {
			// TODO: handle exception

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
		}
		return null;
	}
}
