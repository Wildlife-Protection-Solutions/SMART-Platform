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
package org.wcs.smart.asset.ui.views.asset;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.wcs.smart.asset.engine.StatisticsEngine;
import org.wcs.smart.asset.model.AssetDeployment;

/**
 * Wrapper around asset deployment to track deployment and associated
 * statistics.
 * 
 * @author Emily
 *
 */
class AssetDeploymentWrapper {

	private AssetDeployment deployment;
	private HashMap<StatisticsEngine.Statistic, Object> statValues;
	
	public AssetDeploymentWrapper(AssetDeployment deployment) {
		this.deployment = deployment;
	}
	
	public AssetDeployment getDeployment() {
		return deployment;
	}
	
	public void updateDeployment(AssetDeployment deployment) {
		this.deployment = deployment;
	}
	/**
	 * 
	 * @param stat
	 * @return null if stat not found or not loaded
	 */
	public Object getStatistic(StatisticsEngine.Statistic stat) {
		if (statValues == null) return null;
		return statValues.get(stat);
	}
	
	public void addStatistic(Map<StatisticsEngine.Statistic, Object> values) {
		if (statValues == null) statValues = new HashMap<>();
		statValues.putAll(values);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (other instanceof AssetDeploymentWrapper w) {
			return Objects.equals(deployment,  w.deployment);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return deployment.hashCode();
	}
}
