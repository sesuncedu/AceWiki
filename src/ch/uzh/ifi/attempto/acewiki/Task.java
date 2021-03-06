// This file is part of AceWiki.
// Copyright 2008-2012, AceWiki developers.
// 
// AceWiki is free software: you can redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
// 
// AceWiki is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with AceWiki. If
// not, see http://www.gnu.org/licenses/.

package ch.uzh.ifi.attempto.acewiki;

/**
 * This interface describes a task that should be performed asynchronously (because it is
 * potentially time-consuming and should not delay the GUI).
 * 
 * @author Tobias Kuhn
 */
public interface Task {
	
	/**
	 * This method should execute the time-consuming task that is to be performed asynchronously.
	 */
	public void run();
	
	/**
	 * This method should do the GUI update after {@link #run} has finished.
	 */
	public void updateGUI();

}
