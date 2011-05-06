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

package ch.uzh.ifi.attempto.acewiki.gui.editor;

import nextapp.echo.app.ResourceImageReference;
import nextapp.echo.app.WindowPane;
import nextapp.echo.app.event.ActionListener;
import ch.uzh.ifi.attempto.acewiki.Wiki;
import ch.uzh.ifi.attempto.acewiki.aceowl.TrAdjRelation;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.ape.FunctionWords;
import ch.uzh.ifi.attempto.echocomp.TextField;
import ch.uzh.ifi.attempto.preditor.WordEditorWindow;

/**
 * This class represents a form to create or modify transitive adjectives.
 * 
 * @author Tobias Kuhn
 */
public class TrAdjForm extends FormPane {
	
	private static final long serialVersionUID = -4367996031949560664L;

	private TextField trAdjField = new TextField(this);
	
	private TrAdjRelation relation;
	
	/**
	 * Creates a new form for transitive adjectives.
	 * 
	 * @param relation The relation that is represented by the transitive adjective.
	 * @param window The host window of the form.
	 * @param wiki The wiki instance.
	 * @param actionListener The actionlistener.
	 */
	public TrAdjForm(TrAdjRelation relation, WindowPane window, Wiki wiki, ActionListener actionListener) {
		super("Transitive Adjective", relation != null, window, wiki, actionListener);
		if (relation == null) {
			relation = new TrAdjRelation();
		}
		this.relation = relation;

		setExplanationComponent(
				new ResourceImageReference("ch/uzh/ifi/attempto/acewiki/gui/img/relation.png"),
				"Every transitive adjective represents a certain relation between things. " +
					"For example, the transitive adjective \"located in\" relates things to " +
					"their location. Transitive adjectives consist of an adjective that " +
					"is followed by a preposition."
			);
		addRow("tr. adjective", trAdjField, "examples: located in, matched with, fond of", true);
		
		trAdjField.setText(relation.getPrettyWord(0));
	}
	
	/**
	 * Creates a new creator window for transitive adjectives.
	 * 
	 * @param wiki The wiki instance.
	 * @param actionListener The actionlistener.
	 * @return The new creator window.
	 */
	public static WordEditorWindow createCreatorWindow(Wiki wiki, ActionListener actionListener) {
		WordEditorWindow creatorWindow = new WordEditorWindow("Word Creator");
		creatorWindow.addTab(new TrAdjForm(null, creatorWindow, wiki, actionListener));
		return creatorWindow;
	}
	
	/**
	 * Creates a new editor window for transitive adjectives.
	 * 
	 * @param relation The relation that is represented by the transitive adjective that should be edited.
	 * @param wiki The wiki instance.
	 * @return The new editor window.
	 */
	public static WordEditorWindow createEditorWindow(TrAdjRelation relation, Wiki wiki) {
		WordEditorWindow editorWindow = new WordEditorWindow("Word Editor");
		editorWindow.addTab(new TrAdjForm(relation, editorWindow, wiki, wiki));
		return editorWindow;
	}

	public OntologyElement getOntologyElement() {
		return relation;
	}

	protected void save() throws InvalidWordException {
		Wiki wiki = getWiki();
		String name = normalize(trAdjField.getText());
		String nameP = name.replace("_", " ");
		
		if (name.equals("")) {
			throw new InvalidWordException("No word defined: Please specify the transitive " +
				"adjective.");
		}
		if (!isValidWordOrEmpty(name)) {
			throw new InvalidWordException("Invalid character: Only a-z, A-Z, 0-9, -, and " +
				"spaces are allowed, and the first character must be one of a-z A-Z.");
		}
		if (FunctionWords.isFunctionWord(name)) {
			throw new InvalidWordException("'" + nameP + "' is a predefined word and cannot be " +
				"used here.");
		}
		OntologyElement oe = wiki.getOntology().getElement(name);
		if (oe != null && oe != relation) {
			throw new InvalidWordException("The word '" + nameP + "' is already used. Please " +
				"use a different one.");
		}
		relation.setWords(name);
		wiki.log("edit", "transitive adjective: " + name);
		if (relation.getOntology() == null) {
			relation.registerAt(getWiki().getOntology());
		}
		finished(relation);
	}

}
