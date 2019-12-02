/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.engine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Represents a PAWS task json object
 * 
 * @author Emily
 *
 */
public class PawsTask {

	private String taskId;
	private String status;
	private String backendstatus; //created, failed,  running
	private String endpoint;
	private String endpointpath;
	private LocalDateTime timestamp;
	private Boolean publishToGrid;
	private String body;
	
	
	private PawsTask(String taskId, String status, String backendstatus, String endpoint, String endpointpath, LocalDateTime timestamp, Boolean publishToGrid, String body) {
		this.taskId = taskId;
		this.status = status;
		this.endpoint = endpoint;
		this.timestamp = timestamp;
		
		this.backendstatus = backendstatus;
		this.endpointpath = endpointpath;
		this.publishToGrid = publishToGrid;
		this.body = body;
	}
	
	public String getBody() { return this.body; }
	public String getTaskId() { return this.taskId; }
	public String getStatus() { return this.status; }
	public String getBackendStatus() { return this.backendstatus; }
	public String getEndPoint() { return this.endpoint; }
	public String getEndPointPath() { return this.endpointpath; }
	public LocalDateTime getTimestamp() { return this.timestamp; }
	public Boolean getPublishToGrid() { return this.publishToGrid; }
	
	
	public static PawsTask parse(String json) throws Exception {
		
		System.out.println("result:" + json);
		System.out.println("---------");
		
		JSONParser parse = new JSONParser();
		JSONObject item = (JSONObject) parse.parse(json);
		
		String tid = (String) item.get("TaskId");
		if (tid == null) throw new Exception("Task Id not provided.");
		String status = (String) item.get("Status");
		String endpoint = (String) item.get("Endpoint");
		String endpointpath = (String) item.get("EndpointPath");
		String body = (String) item.get("Body");
		String backendstatus = (String)item.get("BackendStatus");
		
		Boolean publishtogrid = (Boolean)item.get("PublishToGrid");
		
		LocalDateTime d = null;
		try {
			d = DateTimeFormatter.ofPattern("M/d/yyyy h:m:s a").parse((String)item.get("Timestamp"), LocalDateTime::from);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return new PawsTask(tid, status, backendstatus, endpoint, endpointpath, d, publishtogrid, body);
	}
}
