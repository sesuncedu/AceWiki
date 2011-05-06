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
import ch.uzh.ifi.attempto.acewiki.aceowl.NounConcept;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.ape.FunctionWords;
import ch.uzh.ifi.attempto.echocomp.TextField;
import ch.uzh.ifi.attempto.preditor.WordEditorWindow;

/**
 * This class represents a form to create or modify nouns.
 * 
 * @author Tobias Kuhn
 */
public class NounForm extends FormPane {
	
	private static final long serialVersionUID = 172544159284997517L;
	
	private TextField singularField = new TextField(this);
	private TextField pluralField = new TextField(this);
	
	private NounConcept concept;
	private int wordNumber;
	
	/**
	 * Creates a new noun form.
	 * 
	 * @param concept The concept that is represented by the noun.
	 * @param wordNumber The word form id (only used if called from the sentence editor).
	 * @param window The host window of the form.
	 * @param wiki The wiki instance.
	 * @param actionListener
	 */
	public NounForm(NounConcept concept, int wordNumber, WindowPane window, Wiki wiki,
			ActionListener actionListener) {
		super("Noun", concept != null, window, wiki, actionListener);
		if (concept == null) {
			concept = new NounConcept();
		}
		this.concept = concept;
		
		this.wordNumber = wordNumber;

		setExplanationComponent(
				new ResourceImageReference("ch/uzh/ifi/attempto/acewiki/gui/img/concept.png"),
				"Every noun represents a certain type of things. " +
					"For example, the noun \"city\" stands for all things that are cities."
			);
		addRow("singular", singularField, "examples: woman, city, process", true);
		addRow("plural", pluralField, "examples: women, cities, processes", true);
		
		singularField.setText(concept.getPrettyWord(0));
		pluralField.setText(concept.getPrettyWord(1));
	}
	
	/**
	 * Creates a new creator window for nouns.
	 * 
	 * @param wordNumber The word form id (only used if called from the sentence editor).
	 * @param wiki The wiki instance.
	 * @param actionListener The actionlistener.
	 * @return The new creator window.
	 */
	public static WordEditorWindow createCreatorWindow(int wordNumber, Wiki wiki,
			ActionListener actionListener) {
		WordEditorWindow creatorWindow = new WordEditorWindow("Word Creator");
		creatorWindow.addTab(new NounForm(
				null,
				wordNumber,
				creatorWindow,
				wiki,
				actionListener
			));
		return creatorWindow;
	}
	
	/**
	 * Creates a new editor window for nouns.
	 * 
	 * @param concept The concept that is represented by the noun that should be edited.
	 * @param wiki The wiki instance.
	 * @return The new editor window.
	 */
	public static WordEditorWindow createEditorWindow(NounConcept concept, Wiki wiki) {
		WordEditorWindow editorWindow = new WordEditorWindow("Word Editor");
		editorWindow.addTab(new NounForm(concept, 0, editorWindow, wiki, wiki));
		return editorWindow;
	}

	public OntologyElement getOntologyElement() {
		return concept;
	}

	protected void save() throws InvalidWordException {
		Wiki wiki = getWiki();
		String singular = normalize(singularField.getText());
		String plural = normalize(pluralField.getText());
		String singularP = singular.replace("_", " ");
		String pluralP = plural.replace("_", " ");
		
		if (singular.equals(plural)) {
			throw new InvalidWordException("Singular and plural form have to be distinct.");
		}
		if (singular.equals("")) {
			throw new InvalidWordException("No singular form defined: Please specify the " +
				"singular form.");
		}
		if (!isValidWordOrEmpty(singular)) {
			throw new InvalidWordException("Invalid character: Only a-z, A-Z, 0-9, -, and " +
				"spaces are allowed, and the first character must be one of a-z A-Z.");
		}
		if (FunctionWords.isFunctionWord(singular)) {
			throw new InvalidWordException("'" + singularP + "' is a predefined word and cannot " +
				"be used here.");
		}
		OntologyElement oe = wiki.getOntology().getElement(singular);
		if (oe != null && oe != concept) {
			throw new InvalidWordException("The word '" + singularP + "' is already used. " +
				"Please use a different one.");
		}
		if (plural.equals("")) {
			throw new InvalidWordException("No plural form defined: Please specify the plural " +
				"form.");
		}
		if (!isValidWordOrEmpty(plural)) {
			throw new InvalidWordException("Invalid character: Only a-z, A-Z, 0-9, -, and " +
				"spaces are allowed, and the first character must be one of a-z A-Z.");
		}
		if (FunctionWords.isFunctionWord(plural)) {
			throw new InvalidWordException("'" + pluralP + "' is a predefined word and cannot " +
				"be used here.");
		}
		oe = wiki.getOntology().getElement(plural);
		if (oe != null && oe != concept) {
			throw new InvalidWordException("The word '" + pluralP + "' is already used. Please " +
				"use a different one.");
		}
		concept.setWords(singular, plural);
		wiki.log("edit", "noun: " + singular + " / " + plural);
		if (concept.getOntology() == null) {
			concept.registerAt(getWiki().getOntology());
		}
		finished(concept, wordNumber);
	}

}
