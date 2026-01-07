/*
 * This file is part of the L2ClientDat project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.actions;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.l2jmobius.L2ClientDat;

public abstract class ActionTask extends Task<Void>
{
	protected final L2ClientDat _l2clientdat;
	private double _progress = 0.0;
	private int _lastProgressValue = 0;
	
	public ActionTask(L2ClientDat l2clientdat)
	{
		_l2clientdat = l2clientdat;
		progressProperty().addListener((observable, oldValue, newValue) -> _l2clientdat.onProgressTask((int) Math.round(newValue.doubleValue() * 100.0)));
		setOnRunning(event -> _l2clientdat.onStartTask());
		setOnSucceeded(event -> _l2clientdat.onStopTask());
		setOnCancelled(event -> _l2clientdat.onStopTask());
		setOnFailed(event -> _l2clientdat.onStopTask());
	}
	
	public L2ClientDat getL2ClientDat()
	{
		return _l2clientdat;
	}
	
	@Override
	protected Void call()
	{
		updateProgress(0, 100);
		action();
		if (!isCancelled())
		{
			updateProgress(100, 100);
		}
		return null;
	}
	
	protected abstract void action();
	
	public final void abort()
	{
		if (isCancelled())
		{
			return;
		}
		
		cancel();
		Platform.runLater(_l2clientdat::onAbortTask);
	}
	
	public double addProgress(double progress, double value, double weight)
	{
		changeProgress(_progress = getWeightValue(value, weight) + progress);
		return _progress;
	}
	
	public void changeProgress(double value)
	{
		final int intValue = (int) Math.max(0.0, Math.min(100.0, value));
		if (intValue > _lastProgressValue)
		{
			_lastProgressValue = intValue;
			updateProgress(intValue, 100);
		}
	}
	
	public double getCurrentProgress()
	{
		return _progress;
	}
	
	public double getWeightValue(double value, double weight)
	{
		return (value / 100.0) * weight;
	}
	
}
