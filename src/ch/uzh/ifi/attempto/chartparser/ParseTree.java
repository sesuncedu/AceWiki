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

package ch.uzh.ifi.attempto.chartparser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the parse tree of a successfully parsed text.
 * 
 * @author Tobias Kuhn
 */
public class ParseTree {
	
	private Edge topNode;
	private String lamFunctor = "lam";
	private String appFunctor = "app";
	private String semLabel = "sem";
	
	/**
	 * Creates a new parse tree object.
	 * 
	 * @param topNode The top node.
	 */
	ParseTree(Edge topNode) {
		this.topNode = topNode.deepCopy(true);
	}
	
	/**
	 * Sets the name of the annotation item that contains the semantics information. The default is
	 * "sem".
	 * 
	 * @param semLabel The name of the annotation item containing the semantics.
	 */
	public void setSemanticsLabel(String semLabel) {
		this.semLabel = semLabel;
	}
	
	/**
	 * Sets the functor of the lambda function for the calculation of lambda semantics. The default
	 * is "lam".
	 * 
	 * @param lamFunctor The lambda functor.
	 */
	public void setLambdaFunctor(String lamFunctor) {
		this.lamFunctor = lamFunctor;
	}
	
	/**
	 * Sets the functor of the application function for the calculation of lambda semantics. The
	 * default is "app".
	 * 
	 * @param appFunctor The application functor.
	 */
	public void setApplicationFunctor(String appFunctor) {
		this.appFunctor = appFunctor;
	}
	
	/**
	 * Returns the top node of the parse tree. The nodes of the tree are represented by edges, each
	 * of which has links to its child edges.
	 * 
	 * @return The top node of the parse tree.
	 */
	public Edge getTopNode() {
		return topNode;
	}
	
	/**
	 * Returns the syntax tree. The leaves of the tree are objects of Category. All other nodes are
	 * arrays of Object containing the child nodes.
	 * 
	 * @return The syntax tree.
	 */
	public Object getSynTree() {
		return getSynTree(topNode);
	}
	
	private Object getSynTree(Edge edge) {
		if (edge == null) {
			return null;
		} else {
			Category h = edge.getHead();
			if (h instanceof Terminal) {
				return h;
			} else if (h instanceof Preterminal) {
				return new Object[] {h, edge.getBody()[0]};
			} else {
				List<Edge> c = edge.getChildren();
				Object[] o = new Object[c.size()+1];
				o[0] = h;
				for (int i = 0 ; i < c.size() ; i++) {
					o[i+1] = getSynTree(c.get(i));
				}
				return o;
			}
		}
	}
	
	/**
	 * Returns a serialization of the syntax tree.
	 * 
	 * @return A serialization of the syntax tree.
	 */
	public String getSerializedSynTree() {
		return serializeStructure(getSynTree());
	}
	
	/**
	 * Returns an ASCII representation of the syntax tree.
	 * 
	 * @return An ASCII representation of the syntax tree.
	 */
	public String getAsciiSynTree() {
		return structureToAsciiTree(getSynTree(), 0);
	}

	/**
	 * Returns the semantics tree. The semantics are retrieved from the annotations of the grammar
	 * rules. The leaves of the tree are objects of String or StringRef. All other nodes are arrays
	 * of Object containing the child nodes.
	 * 
	 * @return The semantics tree.
	 */
	public Object getSemTree() {
		return getSemTree(topNode);
	}
	
	private Object getSemTree(Edge parseTree) {
		if (parseTree == null) return null;
		Object structure =  parseTree.getAnnotation().getItem(semLabel);
		if (structure == null) return null;
		for (Edge e : parseTree.getChildren()) {
			Object o = getSemTree(e);
			if (o != null) {
				structure = new Object[] {appFunctor, structure, o};
			}
		}
		return structure;
	}

	/**
	 * Returns a serialization of the semantics tree.
	 * 
	 * @return A serialization of the semantics tree.
	 */
	public String getSerializedSemTree() {
		return serializeStructure(getSemTree());
	}

	/**
	 * Returns an ASCII representation of the semantics tree.
	 * 
	 * @return An ASCII representation of the semantics tree.
	 */
	public String getAsciiSemTree() {
		return structureToAsciiTree(getSemTree(), 0);
	}

	/**
	 * Returns the semantics tree, interpreted as a lambda expression. The returned tree is beta-
	 * reduced. The semantics are retrieved from the annotations of the grammar rules. The leaves
	 * of the tree are objects of String or StringRef. All other nodes are arrays of Object
	 * containing the child nodes.
	 * 
	 * @return The beta-reduced semantics tree.
	 */
	public Object getLambdaSemTree() {
		Object o = getSemTree();
		Map<Integer, Object> replace = new HashMap<Integer, Object>();
		replace.put(-1, "");
		while (replace.containsKey(-1)) {
			replace.remove(-1);
			o = applyBetaReduction(o, replace);
		}
		return o;
	}

	/**
	 * Returns a serialization of the semantics tree under lambda interpretation.
	 * 
	 * @return A serialization of the lambda semantics tree.
	 */
	public String getSerializedLambdaSemTree() {
		return serializeStructure(getLambdaSemTree());
	}

	/**
	 * Returns an ASCII representation of the semantics tree under lambda interpretation.
	 * 
	 * @return An ASCII representation of the lambda semantics tree.
	 */
	public String getAsciiLambdaSemTree() {
		return structureToAsciiTree(getLambdaSemTree(), 0);
	}
	
	private Object applyBetaReduction(Object obj, Map<Integer, Object> replace) {
		if (obj == null) {
			return null;
		} else if (obj instanceof String) {
			return obj;
		} else if (obj instanceof StringRef) {
			StringRef sr = (StringRef) obj;
			if (replace.containsKey(sr.getID())) {
				replace.put(-1, "");
				return applyBetaReduction(replace.get(sr.getID()), replace);
			} else {
				return obj;
			}
		} else if (obj instanceof Object[]) {
			Object[] a = (Object[]) obj;
			if (a.length == 0) {
				return obj;
			}
			Object[] c = new Object[a.length];
			for (int i = 0 ; i < a.length ; i++) {
				c[i] = applyBetaReduction(a[i], replace);
			}
			if (a.length == 3 && appFunctor.equals(a[0]) && a[1] instanceof Object[]) {
				Object[] l = (Object[]) a[1];
				if (l.length == 3 && lamFunctor.equals(l[0]) && l[1] instanceof StringRef) {
					replace.put(((StringRef) l[1]).getID(), a[2]);
					replace.put(-1, "");
					return applyBetaReduction(l[2], replace);
				}
			}
			return c;
		}
		return obj;
	}
	
	private String serializeStructure(Object obj) {
		if (obj instanceof Object[]) {
			Object[] a = (Object[]) obj;
			if (a.length == 0) {
				return "*empty*";
			} else if (a.length == 1) {
				return elementToString(a[0]);
			} else {
				String s = elementToString(a[0]) + "(";
				for (int i = 1 ; i < a.length ; i++) {
					s += serializeStructure(a[i]) + ", ";
				}
				if (s.endsWith(", ")) s = s.substring(0, s.length()-2);
				return s + ")";
			}
		} else {
			return elementToString(obj);
		}
	}
	
	private String structureToAsciiTree(Object obj, int tab) {
		String t = "";
		for (int i=0 ; i < tab ; i++) t += "  ";
		if (obj instanceof Object[]) {
			Object[] a = (Object[]) obj;
			if (a.length == 0) {
				t += "*empty*\n";
			} else {
				t += elementToString(a[0]) + "\n";
				for (int i = 1 ; i < a.length ; i++) {
					t += structureToAsciiTree(a[i], tab+1);
				}
			}
		} else {
			return t + elementToString(obj) + "\n";
		}
		return t;
	}
	
	private String elementToString(Object obj) {
		if (obj == null) {
			return "*null*";
		} else if (obj instanceof String) {
			String s = (String) obj;
			if (s.matches("[a-zA-Z0-9_]+")) {
				return s;
			} else {
				return "'" + s + "'";
			}
		} else if (obj instanceof StringRef) {
			StringRef sr = (StringRef) obj;
			if (sr.getString() == null) {
				return "?" + sr.getID();
			} else {
				return elementToString(sr.getString());
			}
		} else if (obj instanceof Terminal) {
			return obj.toString();
		} else if (obj instanceof Preterminal) {
			return "$" + ((Preterminal) obj).getName();
		} else if (obj instanceof Category) {
			return ((Category) obj).getName();
		}
		return "*invalid*";
	}

}
