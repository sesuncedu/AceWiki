// This file is part of AceWiki.
// Copyright 2008-2011, Tobias Kuhn.
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

package ch.uzh.ifi.attempto.acewiki.gui;

import nextapp.echo.app.Alignment;
import nextapp.echo.app.Column;
import nextapp.echo.app.Extent;
import nextapp.echo.app.Font;
import nextapp.echo.app.Grid;
import nextapp.echo.app.Insets;
import nextapp.echo.app.Row;
import nextapp.echo.app.WindowPane;
import nextapp.echo.app.event.ActionEvent;
import nextapp.echo.app.event.ActionListener;
import nextapp.echo.app.layout.GridLayoutData;
import ch.uzh.ifi.attempto.acewiki.Wiki;
import ch.uzh.ifi.attempto.acewiki.core.user.User;
import ch.uzh.ifi.attempto.echocomp.GeneralButton;
import ch.uzh.ifi.attempto.echocomp.Label;
import ch.uzh.ifi.attempto.echocomp.MessageWindow;
import ch.uzh.ifi.attempto.echocomp.PasswordField;
import ch.uzh.ifi.attempto.echocomp.SolidLabel;
import ch.uzh.ifi.attempto.echocomp.Style;
import ch.uzh.ifi.attempto.echocomp.TextField;
import ch.uzh.ifi.attempto.echocomp.VSpace;

/**
 * This class represents a window with information about the current user.
 * 
 * @author Tobias Kuhn
 */
public class UserWindow extends WindowPane implements ActionListener {
	
	private static final long serialVersionUID = -6704597832001286479L;
	
	private Wiki wiki;
	private User user;
	
	private TextField emailField = new TextField(220, this, Font.ITALIC);
	private PasswordField passwordField = new PasswordField(220, this);
	private PasswordField newPasswordField = new PasswordField(220, this);
	private PasswordField retypePasswordField = new PasswordField(220, this);
	
	private Row buttonBar;
	
	/**
	 * Creates a new user window.
	 * 
	 * @param wiki The wiki instance.
	 */
	public UserWindow(Wiki wiki) {
		this.wiki = wiki;
		this.user = wiki.getUser();
		
		setTitle("User: " + user.getName());
		setTitleFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(13)));
		setModal(true);
		setWidth(new Extent(470));
		setHeight(new Extent(360));
		setResizable(false);
		setMovable(true);
		setTitleBackground(Style.windowTitleBackground);
		setStyleName("Default");
		
		wiki.log("logi", "registration window");

		Grid mainGrid = new Grid(1);
		mainGrid.setInsets(new Insets(10, 10, 10, 0));
		mainGrid.setColumnWidth(0, new Extent(450));
		mainGrid.setRowHeight(0, new Extent(270));
		
		Column messageColumn = new Column();
		GridLayoutData layout1 = new GridLayoutData();
		layout1.setAlignment(new Alignment(Alignment.LEFT, Alignment.TOP));
		messageColumn.setLayoutData(layout1);
		Label label = new Label("Your personal information:");
		label.setFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(13)));
		messageColumn.add(label);
		messageColumn.add(new VSpace());
		
		Grid formGrid = new Grid(2);
		formGrid.setInsets(new Insets(10, 10, 10, 0));
		formGrid.add(new SolidLabel("username:", Font.ITALIC));
		formGrid.add(new SolidLabel(user.getName(), Font.ITALIC));
		formGrid.add(new SolidLabel("registration:", Font.ITALIC));
		formGrid.add(new SolidLabel(user.getUserData("registerdate"), Font.ITALIC));
		formGrid.add(new SolidLabel("number of logins:", Font.ITALIC));
		formGrid.add(new SolidLabel(user.getUserData("logincount"), Font.ITALIC));
		formGrid.add(new SolidLabel("email:", Font.ITALIC));
		emailField.setText(user.getUserData("email"));
		emailField.setEnabled(false);
		formGrid.add(emailField);
		formGrid.add(new SolidLabel("current password:", Font.ITALIC));
		passwordField.setText("***************");
		passwordField.setEnabled(false);
		formGrid.add(passwordField);
		formGrid.add(new SolidLabel("new password:", Font.ITALIC));
		newPasswordField.setEnabled(false);
		formGrid.add(newPasswordField);
		formGrid.add(new SolidLabel("retype new password:", Font.ITALIC));
		retypePasswordField.setEnabled(false);
		formGrid.add(retypePasswordField);
		messageColumn.add(formGrid);
		
		mainGrid.add(messageColumn);

		buttonBar = new Row();
		buttonBar.setCellSpacing(new Extent(10));
		buttonBar.setInsets(new Insets(0, 0, 0, 10));
		buttonBar.add(new GeneralButton("Unlock", 80, this));
		buttonBar.add(new GeneralButton("Close", 80, this));
		GridLayoutData layout2 = new GridLayoutData();
		layout2.setAlignment(new Alignment(Alignment.CENTER, Alignment.BOTTOM));
		buttonBar.setLayoutData(layout2);
		mainGrid.add(buttonBar);
		
		add(mainGrid);
	}

	public void actionPerformed(ActionEvent e) {
		String password = passwordField.getText();
		String newPassword = newPasswordField.getText();
		String newPassword2 = retypePasswordField.getText();
		String email = emailField.getText();
		if ("Cancel".equals(e.getActionCommand()) || "Close".equals(e.getActionCommand())) {
			wiki.log("logi", "registration canceled");
			setVisible(false);
			wiki.removeWindow(this);
		} else if ("Unlock".equals(e.getActionCommand())) {
			emailField.setEnabled(true);
			passwordField.setEnabled(true);
			passwordField.setText("");
			newPasswordField.setEnabled(true);
			retypePasswordField.setEnabled(true);
			buttonBar.removeAll();
			buttonBar.add(new GeneralButton("Change", 80, this));
			buttonBar.add(new GeneralButton("Cancel", 80, this));
			wiki.getApplication().setFocusedComponent(emailField);
		} else {
			wiki.log("logi", "pressed: change user data");
			if (!user.isCorrectPassword(password)) {
				wiki.log("logi", "invalid password");
				if (password.length() == 0) {
					wiki.showWindow(new MessageWindow(
							"Error",
							"You have to enter your current password to apply the changes.",
							"OK"
						));
				} else {
					wiki.showWindow(new MessageWindow(
							"Error",
							"The password is not correct.",
							"OK"
						));
				}
			} else if (newPassword.length() > 0 && newPassword.length() < 5) {
				wiki.log("logi", "invalid new password");
				wiki.showWindow(new MessageWindow(
						"Error",
						"Password needs at least 5 characters.",
						"OK"
					));
			} else if (!newPassword.equals(newPassword2)) {
				wiki.log("logi", "retype password does not match");
				wiki.showWindow(new MessageWindow(
						"Error",
						"The two passwords do not match.",
						"OK"
					));
			} else if (email.indexOf("@") < 0) {
				wiki.log("logi", "no email");
				wiki.showWindow(new MessageWindow(
						"Error",
						"Please provide a valid email address.",
						"OK"
					));
			} else {
				user.setUserData("email", email);
				if (newPassword.length() > 0) {
					user.changePassword(password, newPassword);
				}
				wiki.removeWindow(this);
			}
		}
	}
	
}
