// This file is part of AceWiki.
// Copyright 2008-2010, Tobias Kuhn.
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

package ch.uzh.ifi.attempto.acewiki.core.ontology;

import static ch.uzh.ifi.attempto.ape.OutputType.DRSPP;
import static ch.uzh.ifi.attempto.ape.OutputType.OWLRDF;
import static ch.uzh.ifi.attempto.ape.OutputType.OWLXML;
import static ch.uzh.ifi.attempto.ape.OutputType.PARAPHRASE1;
import static ch.uzh.ifi.attempto.ape.OutputType.SYNTAX;
import static ch.uzh.ifi.attempto.ape.OutputType.SYNTAXPP;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ch.uzh.ifi.attempto.ape.ACEParserResult;
import ch.uzh.ifi.attempto.ape.APELocal;
import ch.uzh.ifi.attempto.ape.Lexicon;
import ch.uzh.ifi.attempto.ape.LexiconEntry;
import ch.uzh.ifi.attempto.ape.MessageContainer;
import ch.uzh.ifi.attempto.preditor.ContextChecker;
import ch.uzh.ifi.attempto.preditor.EnglishContextChecker;
import ch.uzh.ifi.attempto.preditor.TextContainer;
import ch.uzh.ifi.attempto.preditor.TextElement;

/**
 * This class represents an ACE sentence, which can be either a declaration (declarative sentence)
 * or a question.
 *<p>
 * ACE sentences can either have an ontology element as owner (in the case of asserted sentences or
 * questions) or can be independent statements with no owner (in the case of inferred sentences).
 *<p>
 * Parsing of the sentence is done lazily, i.e. at the first time when a parsing result is
 * required. Parsing fails silently. No exceptions are thrown if a sentence is not ACE compliant.
 * 
 * @author Tobias Kuhn
 */
public abstract class Sentence extends Statement {
	
	/**
	 * The context checker used for AceWiki.
	 */
	public static final ContextChecker contextChecker = new EnglishContextChecker(true, true);
	
	private static OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	
	private String text;
	private boolean integrated = false;
	
	// These fields are evaluated lazily:
	private TextContainer textContainer;
	private ACEParserResult parserResult;
	private String owlxml;
	private Boolean reasonerParticipant;
	private Boolean isOWL;
	private Boolean isOWLSWRL;
	private Set<OWLAxiom> owlAxioms;
	
	/**
	 * Initializes a new sentence with the given ontology element as its owner.
	 * 
	 * @param text The sentence text.
	 * @param owner The owner ontology element.
	 */
	protected Sentence(String text, OntologyElement owner) {
		super(owner);
		this.text = text;
	}
	
	/**
	 * Initializes a new independent sentence.
	 * 
	 * @param text The sentence text.
	 * @param ontology The ontology.
	 */
	protected Sentence(String text, Ontology ontology) {
		super(ontology);
		this.text = text;
	}
	
	/**
	 * Returns a list of text elements that represent the tokens of this sentence.
	 * 
	 * @return A token list.
	 */
	public List<TextElement> getTextElements() {
		if (textContainer == null) {
			tokenize();
		}
		return textContainer.getTextElements();
	}
	
	/**
	 * Returns the sentence text as a string. Underscores are used for compound words,
	 * e.g. "credit_card".
	 * 
	 * @return The sentence text as a string.
	 */
	public String getText() {
		if (textContainer == null) {
			tokenize();
		}
		return getUnderscoredText(textContainer);
	}
	
	/**
	 * Returns the sentence text as a string with underscores displayed as blanks. Compound
	 * words containing underscores like "credit_cards" are pretty-printed with blank characters:
	 * "credit card".
	 * 
	 * @return The sentence text as a pretty-printed string.
	 */
	public String getPrettyText() {
		return textContainer.getText();
	}
	
	/**
	 * Returns the parser result object.
	 * 
	 * @return The parser result object.
	 */
	public ACEParserResult getParserResult() {
		if (parserResult == null) {
			parse();
		}
		return parserResult;
	}
	
	/**
	 * Returns the OWL/XML representation of this sentence as a string.
	 * 
	 * @return The OWL/XML representation.
	 */
	public String getOWLXML() {
		if (owlxml == null) {
			parse();
		}
		return owlxml;
	}
	
	/**
	 * Returns true if this sentence participates in reasoning.
	 * 
	 * @return true if this sentence participates in reasoning.
	 */
	public boolean isReasonerParticipant() {
		if (reasonerParticipant == null) {
			parse();
		}
		return reasonerParticipant;
	}
	
	/**
	 * Returns true if this sentence has an OWL representation.
	 * 
	 * @return true if this sentence has an OWL representation.
	 */
	public boolean isOWL() {
		if (isOWL == null) {
			parse();
		}
		return isOWL;
	}
	
	/**
	 * Returns true if this sentence has an OWL or SWRL representation.
	 * 
	 * @return true if this sentence has an OWL or SWRL representation.
	 */
	public boolean isOWLSWRL() {
		if (isOWLSWRL == null) {
			parse();
		}
		return isOWLSWRL;
	}
	
	/**
	 * Returns a set of OWL axioms that represent this sentence.
	 * 
	 * @return The OWL axioms.
	 */
	public Set<OWLAxiom> getOWLAxioms() {
		if (owlxml == null) {
			parse();
		}
		if (owlAxioms == null) {
			owlAxioms = new HashSet<OWLAxiom>();
		}
		return owlAxioms;
	}
	
	private void tokenize() {
		textContainer = Tokenizer.tokenize(text, getOntology());
	}
	
	/**
	 * Parses the sentence text. The OWL and SWRL representations are calculated if possible.
	 * This method is called automatically the first time a parsing result is needed.
	 * Furthermore, it needs to be called each time a word form of an ontology element
	 * (that occurs in the sentence) has changed.
	 */
	synchronized void parse() {
		APELocal.getInstance().setURI(getOntology().getURI());
		APELocal.getInstance().setClexEnabled(false);
		Lexicon lexicon = new Lexicon();
		for (TextElement te : getTextElements()) {
			if (te instanceof OntologyTextElement) {
				OntologyElement oe = ((OntologyTextElement) te).getOntologyElement();
				for (LexiconEntry le : oe.getLexiconEntries()) {
					lexicon.addEntry(le);
				}
			}
		}
		parserResult = APELocal.getInstance().getMultiOutput(
				getText(),
				lexicon,
				PARAPHRASE1,
				SYNTAX,
				SYNTAXPP, 
				OWLXML,
				OWLRDF,
				DRSPP
			);
		MessageContainer mc = parserResult.getMessageContainer();
		owlxml = parserResult.get(OWLXML);
		if (owlxml != null) {
			// Every OWL ontology object needs its own URI:
			long hashCode = (long) getText().hashCode() - Integer.MIN_VALUE;
			String uri = getOntology().getURI();
			owlxml = owlxml.replace("URI=\"" + uri + "\">", "URI=\"" + uri + "/" + hashCode + "\">");
			
			// Transform from OWL 1.1 to OWL 2:
			owlxml = owlxml.replaceAll("http://www.w3.org/2006/12/owl11-xml#", "http://www.w3.org/2002/07/owl#");
			owlxml = owlxml.replaceAll("InverseObjectProperty>", "ObjectInverseOf>");
			owlxml = owlxml.replaceAll("SubObjectPropertyChain>", "ObjectPropertyChain>");
			owlxml = owlxml.replaceAll("ObjectExistsSelf>", "ObjectHasSelf>");
			owlxml = owlxml.replaceAll(" URI=\"", " IRI=\"");
			
			//if (this instanceof Question) {
			//	owlxml = owlxml.replace("<Class IRI=\"http://www.w3.org/2002/07/owl#Thing\"/>\n  </SubClassOf>\n</Ontology>",
			//				"<Class IRI=\"http://attempto.ifi.uzh.ch/ace#Question" + hashCode + "\"/>/>\n  </SubClassOf>\n</Ontology>");
			//}
		}
		
		reasonerParticipant =
			(mc.getMessages("owl").size() == 0) &&
			(owlxml.indexOf("<swrl:Imp>") < 0) &&
			(owlxml.length() > 0);
		
		if (reasonerParticipant && getOntology().getGlobalRestrictionsPolicy().equals("noChains")) {
			reasonerParticipant =
				(owlxml.indexOf("<TransitiveObjectProperty>") < 0) &&
				(owlxml.indexOf("<ObjectPropertyChain>") < 0);
		}
		
		isOWL =
			(mc.getMessages("owl").size() == 0) &&
			(owlxml.indexOf("<swrl:Imp>") < 0) &&
			(owlxml.length() > 0);
		isOWLSWRL =
			(mc.getMessages("owl").size() == 0) &&
			(owlxml.length() > 0);
		owlAxioms = null;
		if (isOWL) {
			try {
				OWLOntology owlOntology = createOWLOntology(owlxml);
				if (owlOntology.isEmpty()) {
					reasonerParticipant = false;
					isOWL = false;
					isOWLSWRL = false;
				} else {
					owlAxioms = owlOntology.getAxioms();
				}
			} catch (OWLOntologyCreationException ex) {
				ex.printStackTrace();
			}
		}
		//String messages = mc.toString();
		//if (messages.length() > 0) {
		//	System.err.println("Parser messages: " + messages);
		//}
	}
	
	/**
	 * This method tries to reassert a sentence that is not yet integrated. This is
	 * used for sentences that have an OWL representation but the integration failed
	 * because it introduced an inconsistency. Later, when the ontology has changed,
	 * the integration might succeed.
	 * 
	 * @return An integer value denoting the success/failure of the operation.
	 * @see Ontology#commitSentence(Sentence)
	 */
	public int reassert() {
		int success = getOntology().commitSentence(this);
		getOntology().save(getOwner());
		return success;
	}
	
	/**
	 * Returns true if the sentence is integrated into the ontology.
	 * 
	 * @return true if the sentence is integrated into the ontology.
	 */
	public boolean isIntegrated() {
		return integrated;
	}
	
	void setIntegrated(boolean integrated) {
		this.integrated = integrated;
	}
	
	/**
	 * This method returns whether the sentence can be changed or not. Inferred sentences, for
	 * example, cannot be changed.
	 * 
	 * @return true if this sentence cannot be changed.
	 */
	public abstract boolean isReadOnly();
	
	/**
	 * Checks whether the sentence contains the given word form (by word number) of the
	 * given ontology element.
	 * 
	 * @param e The ontology element.
	 * @param wordNumber The word number.
	 * @return true if the word form occurs in this sentence.
	 */
	public boolean contains(OntologyElement e, int wordNumber) {
		if (textContainer == null) {
			tokenize();
		}
		for (TextElement t : textContainer.getTextElements()) {
			if (t instanceof OntologyTextElement) {
				OntologyTextElement ot = (OntologyTextElement) t;
				if (e == ot.getOntologyElement() && wordNumber == -1) return true;
				if (e == ot.getOntologyElement() && wordNumber == ot.getWordNumber()) return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the sentence contains the given ontology element (no matter which
	 * word form).
	 * 
	 * @param e The ontology element.
	 * @return true if the ontology element occurs in this sentence.
	 */
	public boolean contains(OntologyElement e) {
		return contains(e, -1);
	}
	
	static String getUnderscoredText(TextContainer textContainer) {
		String t = "";
		for (TextElement te : textContainer.getTextElements()) {
			if (te instanceof OntologyTextElement) {
				t += " " + ((OntologyTextElement) te).getUnderscoredText();
			} else if (te.getText().matches("[.?]")) {
				t += te.getText();
			} else {
				t += " " + te.getText();
			}
		}
		if (t.length() > 0) {
			t = t.substring(1);
		}
		return t;
	}
	
	private static OWLOntology createOWLOntology(String owlxml) throws OWLOntologyCreationException {
		OWLOntology o = ontologyManager.loadOntologyFromOntologyDocument(new StringDocumentSource(owlxml));
		ontologyManager.removeOntology(o);
		return o;
	}
	
	String serialize() {
		if (textContainer == null) {
			tokenize();
		}
		String s;
		if (integrated) {
			s = "|";
		} else {
			s = "#";
		}
		for (TextElement te : textContainer.getTextElements()) {
			if (te instanceof OntologyTextElement) {
				OntologyTextElement ot = (OntologyTextElement) te;
				s += " <" + ot.getOntologyElement().getId() + "," + ot.getWordNumber() + ">";
			} else {
				s += " " + te.getText();
			}
		}
		return s + "\n";
	}
	
	public String toString() {
		return getText();
	}

}
