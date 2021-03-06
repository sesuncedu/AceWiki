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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Stack;

import javax.servlet.http.Cookie;

import nextapp.echo.app.Alignment;
import nextapp.echo.app.ApplicationInstance;
import nextapp.echo.app.Color;
import nextapp.echo.app.Column;
import nextapp.echo.app.Component;
import nextapp.echo.app.ContentPane;
import nextapp.echo.app.Extent;
import nextapp.echo.app.Font;
import nextapp.echo.app.Insets;
import nextapp.echo.app.ResourceImageReference;
import nextapp.echo.app.Row;
import nextapp.echo.app.SplitPane;
import nextapp.echo.app.TaskQueueHandle;
import nextapp.echo.app.WindowPane;
import nextapp.echo.app.event.ActionEvent;
import nextapp.echo.app.event.ActionListener;
import nextapp.echo.app.layout.ColumnLayoutData;
import nextapp.echo.webcontainer.ContainerContext;
import ch.uzh.ifi.attempto.acewiki.core.AceWikiDataExporter;
import ch.uzh.ifi.attempto.acewiki.core.AceWikiEngine;
import ch.uzh.ifi.attempto.acewiki.core.AceWikiStorage;
import ch.uzh.ifi.attempto.acewiki.core.LanguageHandler;
import ch.uzh.ifi.attempto.acewiki.core.LexiconTableExporter;
import ch.uzh.ifi.attempto.acewiki.core.Ontology;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.acewiki.core.OntologyExportManager;
import ch.uzh.ifi.attempto.acewiki.core.OntologyExporter;
import ch.uzh.ifi.attempto.acewiki.core.OntologyTextElement;
import ch.uzh.ifi.attempto.acewiki.core.Sentence;
import ch.uzh.ifi.attempto.acewiki.core.Statement;
import ch.uzh.ifi.attempto.acewiki.core.StatementTableExporter;
import ch.uzh.ifi.attempto.acewiki.core.User;
import ch.uzh.ifi.attempto.acewiki.core.UserBase;
import ch.uzh.ifi.attempto.acewiki.gf.GfEngine;
import ch.uzh.ifi.attempto.acewiki.gui.AboutPage;
import ch.uzh.ifi.attempto.acewiki.gui.ArticlePage;
import ch.uzh.ifi.attempto.acewiki.gui.ExportWindow;
import ch.uzh.ifi.attempto.acewiki.gui.FormPane;
import ch.uzh.ifi.attempto.acewiki.gui.GrammarPage;
import ch.uzh.ifi.attempto.acewiki.gui.IconButton;
import ch.uzh.ifi.attempto.acewiki.gui.IndexPage;
import ch.uzh.ifi.attempto.acewiki.gui.ListItem;
import ch.uzh.ifi.attempto.acewiki.gui.LoginWindow;
import ch.uzh.ifi.attempto.acewiki.gui.SearchPage;
import ch.uzh.ifi.attempto.acewiki.gui.SentencePage;
import ch.uzh.ifi.attempto.acewiki.gui.StartPage;
import ch.uzh.ifi.attempto.acewiki.gui.Title;
import ch.uzh.ifi.attempto.acewiki.gui.UserWindow;
import ch.uzh.ifi.attempto.acewiki.gui.WikiPage;
import ch.uzh.ifi.attempto.base.Logger;
import ch.uzh.ifi.attempto.echocomp.EchoThread;
import ch.uzh.ifi.attempto.echocomp.HSpace;
import ch.uzh.ifi.attempto.echocomp.Label;
import ch.uzh.ifi.attempto.echocomp.LocaleResources;
import ch.uzh.ifi.attempto.echocomp.MessageWindow;
import ch.uzh.ifi.attempto.echocomp.SmallButton;
import ch.uzh.ifi.attempto.echocomp.SolidLabel;
import ch.uzh.ifi.attempto.echocomp.Style;
import ch.uzh.ifi.attempto.echocomp.TextAreaWindow;
import ch.uzh.ifi.attempto.echocomp.TextField;
import ch.uzh.ifi.attempto.echocomp.VSpace;
import ch.uzh.ifi.attempto.preditor.PreditorWindow;
import ch.uzh.ifi.attempto.preditor.WordEditorWindow;
import echopoint.externalevent.ExternalEvent;
import echopoint.externalevent.ExternalEventListener;
import echopoint.externalevent.ExternalEventMonitor;

// TODO Get rid of gf package dependency

/**
 * This class represents an AceWiki wiki instance (including its graphical user interface).
 * There is such a wiki object for every wiki user.
 * The actions handled by this class refer to the wiki as a whole.
 *
 * @author Tobias Kuhn
 * @author Kaarel Kaljurand
 */
public class Wiki implements ActionListener, ExternalEventListener {

	private static final long serialVersionUID = 2777443689044226043L;

	private Map<String, String> parameters;

	private final Ontology ontology;
	private final AceWikiEngine engine;
	private String language;
	private User user;
	private OntologyExportManager ontologyExportManager;
	private AceWikiStorage storage;

	private WikiPage currentPage;
	private Column pageCol;
	private ContentPane contentPane = new ContentPane();
	private Row navigationButtons;
	private Logger logger;
	private SplitPane wikiPane;
	private Row loginBackground;

	private IconButton backButton, forwardButton, refreshButton, userButton, logoutButton,
	searchButton;

	private TextField searchTextField = new TextField(170, this);
	private Label userLabel;

	private SmallButton homeButton, indexButton, searchButton2, aboutButton, randomButton,
	newButton, exportButton;

	private SmallButton aboutGrammarButton;

	private List<SmallButton> languageButtons;

	private StartPage startPage;

	private Stack<WikiPage> history = new Stack<WikiPage>();
	private Stack<WikiPage> forward = new Stack<WikiPage>();

	private TaskQueueHandle taskQueue;
	private MessageWindow waitWindow;
	private List<Task> strongTasks = new ArrayList<Task>();
	private List<Task> weakTasks = new ArrayList<Task>();

	private ExternalEventMonitor externalEventMonitor;

	private AceWikiApp application;

	private static Properties properties;

	private boolean disposed = false;

	private boolean locked = false;
	private ActionListener lockedListener;

	/**
	 * Creates a new wiki instance.
	 *
	 * @param backend The backend object.
	 * @param parameters A set of parameters in the form of name/value pairs.
	 * @param sessionID The session id.
	 */
	Wiki(Backend backend, Map<String, String> parameters, int sessionID) {
		this.parameters = parameters;

		storage = backend.getStorage();
		ontology = backend.getOntology();
		engine = ontology.getEngine();

		logger = new Logger(getParameter("context:logdir") + "/" + ontology.getName(), "anon", sessionID);
		application = (AceWikiApp) EchoThread.getActiveApplication();
		taskQueue = application.createTaskQueue();

		language = getParameter("language");
		if (language == null || language.equals("")) {
			language = engine.getLanguages()[0];
		}

		if (isLanguageSwitchingEnabled()) {
			String showLang = getURLParameterValue("showlang");
			if (showLang != null && Arrays.asList(engine.getLanguages()).contains(showLang)) {
				language = showLang;
			}
		}

		application.setLocale(getLocale());

		ontologyExportManager = new OntologyExportManager(ontology);
		for (OntologyExporter o : engine.getExporters()) {
			ontologyExportManager.addExporter(o);
		}
		ontologyExportManager.addExporter(new LexiconTableExporter());
		ontologyExportManager.addExporter(new StatementTableExporter());
		ontologyExportManager.addExporter(new AceWikiDataExporter());

		userLabel = new SolidLabel(getGUIText("acewiki_anonymoususer_name"), Font.ITALIC);
		userLabel.setForeground(Color.DARKGRAY);

		buildContentPane();

		logoutButton.setVisible(false);

		startPage = new StartPage(this);

		// auto login
		if (isLoginEnabled()) {
			String userName = getCookie("lastusername");
			boolean stayLoggedIn = getCookie("stayloggedin").equals("true");
			if (getUserBase().containsUser(userName) && stayLoggedIn) {
				String clientToken = getCookie("stayloggedintoken");
				if (clientToken.length() > 0) {
					log("syst", "try auto login...");
					user = getUserBase().autoLogin(userName, clientToken);
					if (user != null) {
						log("syst", "auto login successful: " + user.getName());
						setUser(user);
					} else {
						log("syst", "auto login failed: " + userName);
						clearCookie("stayloggedintoken");
					}
				}
			}
		}

		String showpage = getURLParameterValue("showpage");
		if (showpage != null && ontology.getElement(showpage) != null) {
			setCurrentPage(ArticlePage.create(ontology.getElement(showpage), this));
		} else {
			setCurrentPage(startPage);
		}

		// This thread checks regularly for pending tasks and executes them. Strong tasks take
		// precedence over weak ones.
		EchoThread asyncThread = new EchoThread() {

			public ApplicationInstance getApplication() {
				return application;
			}

			public void run() {
				while (true) {
					try {
						sleep(500);
					} catch (InterruptedException ex) {}

					if (disposed) {
						break;
					}

					Task task = null;
					if (strongTasks.size() > 0) {
						task = strongTasks.remove(0);
					} else if (weakTasks.size() > 0) {
						task = weakTasks.remove(0);
					}

					final Task fTask = task;
					if (fTask != null) {
						task.run();
						application.enqueueTask(taskQueue, new Runnable() {
							public synchronized void run() {
								fTask.updateGUI();
								if (waitWindow != null) {
									removeWindow(waitWindow);
									waitWindow = null;
								}
							}
						});
					}
				}
			}

		};
		asyncThread.setPriority(Thread.MIN_PRIORITY);
		asyncThread.start();

		update();
	}

	private void buildContentPane() {
		if (loginBackground != null) return;

		contentPane.removeAll();

		SplitPane splitPane1 = new SplitPane(SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM);
		splitPane1.setSeparatorPosition(new Extent(50));
		splitPane1.setSeparatorHeight(new Extent(0));

		SplitPane splitPane2 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL_RIGHT_LEFT);
		splitPane2.setSeparatorPosition(new Extent(215));
		splitPane2.setSeparatorWidth(new Extent(0));

		navigationButtons = new Row();
		navigationButtons.setInsets(new Insets(5));
		navigationButtons.setBackground(Style.shadedBackground);

		backButton = new IconButton("back", this);
		forwardButton = new IconButton("forward", this);
		refreshButton = new IconButton("refresh", this);
		userButton = new IconButton("user", this);
		logoutButton = new IconButton("logout", this);
		searchButton = new IconButton("search", this);

		navigationButtons.add(backButton);
		navigationButtons.add(new HSpace(5));
		navigationButtons.add(forwardButton);
		navigationButtons.add(new HSpace(5));
		navigationButtons.add(refreshButton);
		navigationButtons.add(new HSpace(30));
		Row userRow = new Row();
		userRow.add(userButton);
		userRow.add(new HSpace(3));
		userRow.add(userLabel);
		userRow.add(logoutButton);
		userRow.setVisible(isLoginEnabled());
		navigationButtons.add(userRow);

		ContentPane menuBar = new ContentPane();
		menuBar.setBackground(Style.shadedBackground);
		menuBar.add(navigationButtons);

		Row searchRow = new Row();
		searchRow.setInsets(new Insets(5));
		searchRow.setBackground(Style.shadedBackground);
		searchRow.add(searchButton);
		searchRow.add(new HSpace(5));
		searchRow.add(searchTextField);

		ContentPane searchBar = new ContentPane();
		searchBar.setBackground(Style.shadedBackground);
		searchBar.add(searchRow);

		wikiPane = new SplitPane(
				SplitPane.ORIENTATION_HORIZONTAL_LEFT_RIGHT,
				new Extent(145)
				);
		wikiPane.setSeparatorHeight(new Extent(0));

		SplitPane sideBar = new SplitPane(
				SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM,
				new Extent(170)
				);
		sideBar.setSeparatorHeight(new Extent(0));
		sideBar.setBackground(Style.shadedBackground);
		Column iconCol = new Column();
		iconCol.setInsets(new Insets(10, 10, 10, 0));
		iconCol.setCellSpacing(new Extent(1));

		Label logo = new Label(new ResourceImageReference(
				"ch/uzh/ifi/attempto/acewiki/gui/img/AceWikiLogoSmall.png"
				));
		iconCol.add(logo);

		iconCol.add(new VSpace(10));

		ColumnLayoutData layout = new ColumnLayoutData();
		layout.setAlignment(Alignment.ALIGN_CENTER);

		String title = getParameter("title");
		if (title != null && title.length() > 0) {
			Label titleLabel = new Label(title, Font.ITALIC, 14);
			iconCol.add(titleLabel);
			titleLabel.setLayoutData(layout);
			iconCol.add(new VSpace(5));
		}

		if (isReadOnly()) {
			String s = "— " + getGUIText("acewiki_state_readonly") + " —";
			SolidLabel rolabel = new SolidLabel(s, Font.ITALIC);
			rolabel.setFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(10)));
			rolabel.setLayoutData(layout);
			iconCol.add(rolabel);
		}

		sideBar.add(iconCol);

		Column sideCol = new Column();
		sideCol.setInsets(new Insets(10, 0, 0, 10));
		sideCol.setCellSpacing(new Extent(1));

		SolidLabel label = new SolidLabel(getGUIText("acewiki_sidemenu_navigation"), Font.ITALIC);
		label.setFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(10)));
		sideCol.add(label);
		homeButton = new SmallButton(getGUIText("acewiki_page_main"), this, 12);
		indexButton = new SmallButton(getGUIText("acewiki_page_index"), this, 12);
		searchButton2 = new SmallButton(getGUIText("acewiki_page_search"), this, 12);
		aboutButton = new SmallButton(getGUIText("acewiki_page_about"), this, 12);
		randomButton = new SmallButton(getGUIText("acewiki_page_random"), this, 12);
		sideCol.add(new ListItem(homeButton));
		sideCol.add(new ListItem(indexButton));
		sideCol.add(new ListItem(searchButton2));
		sideCol.add(new ListItem(aboutButton));
		sideCol.add(new ListItem(randomButton));

		sideCol.add(new VSpace(10));
		label = new SolidLabel(getGUIText("acewiki_sidemenu_actions"), Font.ITALIC);
		label.setFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(10)));
		sideCol.add(label);
		newButton = new SmallButton(getGUIText("acewiki_action_new"), this, 12);
		exportButton = new SmallButton(getGUIText("acewiki_action_export"), this, 12);
		if (!isReadOnly() && getEngine().getLexicalTypes().length > 0) {
			sideCol.add(new ListItem(newButton));
		}
		sideCol.add(new ListItem(exportButton));

		if (getEngine() instanceof GfEngine) {
			aboutGrammarButton = new SmallButton(getGUIText("acewiki_page_about_grammar"), this, 12);
			sideCol.add(new VSpace(10));
			sideCol.add(new ListItem(aboutGrammarButton));
		}

		languageButtons = new ArrayList<SmallButton>();

		if (isMultilingual() && isLanguageSwitchingEnabled()) {
			// show language switcher

			sideCol.add(new VSpace(10));
			label = new SolidLabel(getGUIText("acewiki_sidemenu_languages"), Font.ITALIC);
			label.setFont(new Font(Style.fontTypeface, Font.ITALIC, new Extent(10)));
			sideCol.add(label);

			for (String lang : engine.getLanguages()) {
				String n = engine.getLanguageHandler(lang).getLanguageName();
				SmallButton b = new SmallButton(n, this, 12);
				if (lang.equals(language)) b.setEnabled(false);
				languageButtons.add(b);
				sideCol.add(new ListItem(b));
			}
		}

		if (externalEventMonitor != null) {
			externalEventMonitor.removeExternalEventListener(this);
			externalEventMonitor.dispose();
		}
		externalEventMonitor = new ExternalEventMonitor();
		externalEventMonitor.addExternalEventListener(this);
		sideCol.add(externalEventMonitor);

		//sideCol.add(new VSpace(20));
		//sideCol.add(new ItalicLabel("Session ID: " + sessionID));

		sideBar.add(sideCol);

		SplitPane splitPane3 = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL_LEFT_RIGHT);
		splitPane3.setSeparatorWidth(new Extent(1));
		splitPane3.setSeparatorColor(Color.BLACK);
		splitPane3.setSeparatorPosition(new Extent(0));
		splitPane3.add(new Label());

		SplitPane splitPane4 = new SplitPane(SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM);
		splitPane4.setSeparatorHeight(new Extent(1));
		splitPane4.setSeparatorColor(Color.BLACK);
		splitPane4.setSeparatorPosition(new Extent(0));
		splitPane4.add(new Label());

		splitPane3.add(splitPane4);
		pageCol = new Column();
		splitPane4.add(pageCol);

		splitPane2.add(searchBar);
		splitPane2.add(menuBar);

		splitPane1.add(splitPane2);
		splitPane1.add(splitPane3);

		wikiPane.add(sideBar);
		wikiPane.add(splitPane1);

		contentPane.add(wikiPane);
	}

	/**
	 * Returns the content pane containing the wiki GUI.
	 *
	 * @return The content pane.
	 */
	public ContentPane getContentPane() {
		return contentPane;
	}

	/**
	 * Returns the application instance object of this wiki.
	 *
	 * @return The application instance.
	 */
	public ApplicationInstance getApplication() {
		return application;
	}

	/**
	 * Returns the value of the given parameter. These parameters are defined in the web.xml file
	 * of the web application.
	 *
	 * @param paramName The parameter name.
	 * @return The value of the parameter.
	 */
	public String getParameter(String paramName) {
		return parameters.get(paramName);
	}

	/**
	 * Returns whether the login features are enabled.
	 *
	 * @return true if login is enabled.
	 */
	public boolean isLoginEnabled() {
		return "yes".equals(getParameter("login"));
	}

	/**
	 * Returns whether login is required for viewing the wiki data.
	 *
	 * @return true if login is required for viewing.
	 */
	public boolean isLoginRequiredForViewing() {
		if (!isLoginEnabled()) return false;
		return "yes".equals(getParameter("login_required"));
	}

	/**
	 * Returns whether login is required for editing the wiki data.
	 *
	 * @return true if login is required for editing.
	 */
	public boolean isLoginRequiredForEditing() {
		if (!isLoginEnabled()) return false;
		if (isLoginRequiredForViewing()) return true;
		return "edit".equals(getParameter("login_required"));
	}

	/**
	 * Returns whether the user registration is open to everyone.
	 *
	 * @return true if the user registration is open.
	 */
	public boolean isUserRegistrationOpen() {
		if (!isLoginEnabled()) return false;
		return !"no".equals(getParameter("register"));
	}

	/**
	 * Returns whether the wiki is in the current situation editable. This depends on the fact
	 * whether a user is logged in and whether login is required for editing the wiki data.
	 *
	 * @return true if the wiki is editable.
	 */
	public boolean isEditable() {
		return (user != null || !isLoginRequiredForEditing());
	}

	/**
	 * Returns true if this wiki is set to be read-only.
	 *
	 * @return true if this wiki is read-only.
	 */
	public boolean isReadOnly() {
		return "on".equals(parameters.get("readonly"));
	}

	/**
	 * Returns true if language switching is enabled.
	 * 
	 * @return true if language switching is enabled.
	 */
	public boolean isLanguageSwitchingEnabled() {
		return !"off".equals(getParameter("language_switching"));
	}

	/**
	 * Returns true if comment feature is enabled.
	 * 
	 * @return true if enabled.
	 */
	public boolean isCommentingEnabled() {
		String s = getParameter("comments");
		return (s == null || s.equals("on"));
	}

	/**
	 * Returns true if comments are disabled and hidden.
	 * 
	 * @return true if hidden.
	 */
	public boolean isCommentHidingEnabled() {
		return "hide".equals(getParameter("comments"));
	}

	/**
	 * Returns true if retract/reassert actions on sentences are enabled.
	 * 
	 * @return true if enabled.
	 */
	public boolean isRetractReassertEnabled() {
		if ("on".equals(getParameter("retractreassert"))) {
			return true;
		} else if ("off".equals(getParameter("retractreassert"))) {
			return false;
		}
		return getEngine().getReasoner() != null;
	}

	public boolean isDetailsPageEnabled() {
		return !"off".equals(getParameter("details_page"));
	}

	public boolean isTranslationsPageEnabled() {
		if (isMultilingual()) {
			return !"off".equals(getParameter("translations_page"));
		}
		return false;
	}

	/**
	 * Shows the window.
	 *
	 * @param window The window to be shown.
	 */
	public void showWindow(WindowPane window) {
		cleanWindows();
		if (window instanceof WordEditorWindow ||
				window instanceof PreditorWindow ||
				window instanceof TextAreaWindow) {
			int c = getContentPane().getComponentCount() - 1;
			window.setPositionX(new Extent(50 + (c % 5)*40));
			window.setPositionY(new Extent(50 + (c % 5)*20));
		}
		getContentPane().add(window);
	}

	/**
	 * Shows a word editor window.
	 *
	 * @param element The ontology element to be edited.
	 */
	public void showEditorWindow(OntologyElement element) {
		WordEditorWindow editorWindow = new WordEditorWindow(getGUIText("acewiki_wordeditor_title"));
		editorWindow.addTab(new FormPane(element, editorWindow, this));
		showWindow(editorWindow);
	}

	/**
	 * Shows a word creator window for the given word type and number.
	 *
	 * @param type The word type.
	 * @param wordNumber The word number.
	 * @param actionListener The actionlistener.
	 */
	public void showCreatorWindow(String type, int wordNumber, ActionListener actionListener) {
		WordEditorWindow creatorWindow = new WordEditorWindow(getGUIText("acewiki_wordeditor_creatortitle"));
		creatorWindow.addTab(new FormPane(type, wordNumber, creatorWindow, this, actionListener));
		showWindow(creatorWindow);
	}

	/**
	 * Removes the window.
	 *
	 * @param window The window to be removed.
	 */
	public void removeWindow(WindowPane window) {
		window.setVisible(false);
		window.dispose();
		cleanWindows();
	}

	private void cleanWindows() {
		for (Component c : getContentPane().getComponents()) {
			if (!c.isVisible()) {
				getContentPane().remove(c);
			}
		}
	}

	/**
	 * Shows the login window.
	 */
	public void showLoginWindow() {
		if (isLoginRequiredForViewing()) {
			getContentPane().removeAll();
			loginBackground = new Row();
			loginBackground.setInsets(new Insets(10, 10));
			loginBackground.setCellSpacing(new Extent(30));
			Label loginBgLogo = new Label(getImage("AceWikiLogoSmall.png"));
			loginBackground.add(loginBgLogo);
			loginBackground.add(new Title(getParameter("title"), true));
			getContentPane().add(loginBackground);
			getContentPane().setBackground(new Color(230, 230, 230));
		}
		showWindow(new LoginWindow(this));
	}

	/**
	 * Switches to the given page.
	 *
	 * @param page The page to switch to.
	 */
	public void showPage(WikiPage page) {
		if (!currentPage.equals(page)) {
			history.push(currentPage);
			if (history.size() > 20) history.remove(0);
			forward.clear();
		}
		setCurrentPage(page);
		log("navi", "goto: " + page);
		update();
	}

	/**
	 * Switches to the page of the given ontology element.
	 *
	 * @param e The ontology element the page of which should be shown.
	 */
	public void showPage(OntologyElement e) {
		showPage(ArticlePage.create(e, this));
	}

	/**
	 * Go to the previous page in the history.
	 */
	public void back() {
		if (history.isEmpty()) return;
		forward.push(currentPage);
		if (forward.size() > 20) forward.remove(0);
		WikiPage page = history.pop();
		setCurrentPage(page);
		log("navi", "back: " + page);
		update();
	}

	/**
	 * Go to the next page in the history.
	 */
	public void forward() {
		if (forward.isEmpty()) return;
		history.push(currentPage);
		if (history.size() > 20) history.remove(0);
		WikiPage page = forward.pop();
		setCurrentPage(page);
		log("navi", "forw: " + page);
		update();
	}

	/**
	 * Show the start page.
	 */
	public void showStartPage() {
		showPage(startPage);
	}

	/**
	 * Show the index page.
	 */
	public void showIndexPage() {
		showPage(new IndexPage(this));
	}

	/**
	 * Show the search page.
	 */
	public void showSearchPage() {
		showPage(new SearchPage(this, ""));
	}

	/**
	 * Show the about page.
	 */
	public void showAboutPage() {
		showPage(new AboutPage(this));
	}

	/**
	 * Show the about grammar page.
	 */
	public void showAboutGrammarPage() {
		if (engine instanceof GfEngine) {
			GfEngine gfEngine = (GfEngine) engine;
			showPage(new GrammarPage(this, gfEngine.getGfGrammar()));
		}
	}

	/**
	 * Returns the ontology;
	 *
	 * @return The ontology.
	 */
	public Ontology getOntology() {
		return ontology;
	}

	/**
	 * Returns the ontology export manager.
	 *
	 * @return The ontology export manager.
	 */
	public OntologyExportManager getOntologyExportManager() {
		return ontologyExportManager;
	}

	/**
	 * Returns the user base for this wiki.
	 *
	 * @return The user base.
	 */
	public UserBase getUserBase() {
		return storage.getUserBase(ontology);
	}

	/**
	 * Returns all ontology elements. The list is a copy of the internal list.
	 *
	 * @return A list of all ontology elements.
	 */
	public List<OntologyElement> getOntologyElements() {
		return ontology.getOntologyElements();
	}

	/**
	 * Updates the GUI.
	 */
	public void update() {
		pageCol.removeAll();
		pageCol.add(currentPage);

		removeExpiredPages(history);
		removeExpiredPages(forward);
		backButton.setEnabled(!history.isEmpty());
		forwardButton.setEnabled(!forward.isEmpty());

		// The commented-out code below checks at every GUI update whether the ontology is consistent or not.
		// If not, a red AceWiki logo is shown. Usually, this case should never occur because we check for
		// consistency after every new statement.
		//if (ontology.isConsistent()) {
		//	logo.setIcon(new ResourceImageReference("ch/uzh/ifi/attempto/acewiki/gui/img/AceWikiLogoSmall.png"));
		//} else {
		//	logo.setIcon(new ResourceImageReference("ch/uzh/ifi/attempto/acewiki/gui/img/AceWikiLogoSmallRed.png"));
		//}
	}

	private void removeExpiredPages(Stack<WikiPage> stack) {
		WikiPage previousPage = null;
		for (WikiPage page : new ArrayList<WikiPage>(stack)) {
			if (page.isExpired() || page.equals(previousPage)) {
				stack.remove(page);
			} else {
				previousPage = page;
			}
		}
		if (stack.size() > 0 && currentPage.equals(stack.peek())) {
			stack.pop();
		}
	}

	public void updateStatement(Statement oldStatement, List<Statement> newStatements) {
		Statement newStatement = null;
		if (newStatements.size() == 1) {
			newStatement = newStatements.get(0);
		}
		updateStatement(oldStatement, newStatement);
	}

	public void updateStatement(Statement oldStatement, Statement newStatement) {
		if (!(newStatement instanceof Sentence)) return;
		Sentence newSentence = (Sentence) newStatement;

		List<WikiPage> pages = new ArrayList<>();
		pages.addAll(history);
		pages.add(currentPage);
		pages.addAll(forward);
		for (WikiPage page : pages) {
			if (!(page instanceof SentencePage)) continue;
			SentencePage sp = (SentencePage) page;
			if (sp.getSentence() == oldStatement) {
				sp.setSentence(newSentence);
			}
		}
	}

	private void setCurrentPage(WikiPage currentPage) {
		this.currentPage = currentPage;
		refresh();
	}

	/**
	 * Refreshes the current page.
	 */
	public void refresh() {
		currentPage.update();
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		String c = e.getActionCommand();

		if (locked) {
			if (lockedListener != null) {
				lockedListener.actionPerformed(new ActionEvent(this, "locked"));
			}
			return;
		}

		if (src == backButton) {
			log("page", "pressed: back");
			back();
		} else if (src == forwardButton) {
			log("page", "pressed: forward");
			forward();
		} else if (src == indexButton) {
			log("page", "pressed: index");
			showIndexPage();
		} else if (src == aboutButton) {
			log("page", "pressed: about");
			showAboutPage();
		} else if (src == aboutGrammarButton) {
			log("page", "pressed: about grammar");
			showAboutGrammarPage();
		} else if (src == homeButton) {
			log("page", "pressed: main page");
			showStartPage();
		} else if (src == randomButton) {
			log("page", "pressed: random page");
			List<OntologyElement> elements = ontology.getOntologyElements();
			if (elements.size() > 0) {
				int r = (new Random()).nextInt(elements.size());
				showPage(elements.get(r));
			} else {
				showStartPage();
			}
		} else if (src == refreshButton) {
			log("page", "pressed: refresh");
			buildContentPane();
			update();
			refresh();
		} else if (src == newButton) {
			log("page", "pressed: new word");
			if (!isEditable()) {
				showLoginWindow();
			} else {
				WordEditorWindow w = new WordEditorWindow(getGUIText("acewiki_wordeditor_creatortitle"));
				for (String t : getEngine().getLexicalTypes()) {
					w.addTab(new FormPane(t, w, this));
				}
				showWindow(w);
			}
		} else if (src == searchButton || src == searchTextField || src == searchButton2) {
			log("page", "pressed: search '" + searchTextField.getText() + "'");
			String s = searchTextField.getText();
			searchTextField.setText("");
			OntologyElement el = ontology.getElement(s.replace(' ', '_'));
			if (el == null) {
				showPage(new SearchPage(this, s));
			} else {
				showPage(el);
			}
		} else if (src == exportButton) {
			showWindow(new ExportWindow(this));
		} else if (src == logoutButton) {
			showWindow(new MessageWindow(
					"acewiki_message_logouttitle",
					"acewiki_message_logout",
					null,
					this,
					"general_action_yes", "general_action_no"
					));
		} else if (src == userButton) {
			if (user == null) {
				showLoginWindow();
			} else {
				showWindow(new UserWindow(this));
			}
		} else if (src instanceof MessageWindow && c.equals("general_action_yes")) {
			logout();
		} else if (src instanceof OntologyTextElement) {
			// for newly generated elements
			OntologyTextElement te = (OntologyTextElement) src;
			log("edit", "new word: " + te.getOntologyElement().getWord());
			showPage(te.getOntologyElement());
		} else if (languageButtons.contains(src)) {
			switchLanguage(engine.getLanguages()[languageButtons.indexOf(src)]);
		}
	}

	public void externalEvent(ExternalEvent e) {
		String p = e.getParameter("page");
		if (p != null) {
			OntologyElement oe = ontology.getElement(e.getParameter("page"));
			if (oe != null) showPage(oe);
		}
		p = e.getParameter("lang");
		if (p != null &&Arrays.asList(engine.getLanguages()).contains(p)) {
			switchLanguage(p);
		}
	}

	/**
	 * Writes the log entry to the log file.
	 *
	 * @param type The type of the log entry.
	 * @param text The text of the log entry.
	 */
	public void log(String type, String text) {
		logger.log(type, text);
	}

	/**
	 * Logs in the given user.
	 *
	 * @param user The user to log in.
	 * @param stayLoggedIn Defines whether the user should stay logged in or not.
	 */
	public void login(User user, boolean stayLoggedIn) {
		log("syst", "login");
		user.setUserData("stayloggedin", stayLoggedIn + "");
		setCookie("stayloggedin", stayLoggedIn + "");
		String stayloggedintoken;
		if (stayLoggedIn) {
			stayloggedintoken = (new Random()).nextLong() + "";
		} else {
			stayloggedintoken = "";
		}
		user.setUserData("stayloggedintoken", stayloggedintoken);
		setCookie("stayloggedintoken", stayloggedintoken);
		setUser(user);
	}

	/**
	 * Logs out the current user.
	 */
	public void logout() {
		log("syst", "logout");
		user.setUserData("stayloggedintoken", "");
		setCookie("stayloggedintoken", "");
		application.logout();
	}

	/**
	 * Returns the user of this wiki object.
	 *
	 * @return The user.
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Sets the user.
	 *
	 * @param user The user.
	 */
	private void setUser(User user) {
		this.user = user;
		logger.setUsername(user.getName());
		userLabel.setForeground(Color.BLACK);
		userLabel.setText(user.getName());
		logoutButton.setVisible(true);
		if (loginBackground != null) {
			getContentPane().removeAll();
			getContentPane().add(wikiPane);
			getContentPane().setBackground(Color.WHITE);
			loginBackground = null;
		}
		setCookie("lastusername", user.getName());
	}

	/**
	 * Sets a cookie on the client.
	 *
	 * @param name The name of the cookie.
	 * @param value The value of the cookie.
	 */
	public void setCookie(String name, String value) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(1000000000);
		getContainerContext().addCookie(cookie);
	}

	/**
	 * Clears the given cookie on the client.
	 *
	 * @param name The name of the cookie.
	 */
	public void clearCookie(String name) {
		getContainerContext().addCookie(new Cookie(name, null));
	}

	/**
	 * Returns the value of the cookie on the client, or "" if there is no such cookie.
	 *
	 * @param name The name of the cookie.
	 * @return The value of the cookie.
	 */
	public String getCookie(String name) {
		for (Cookie cookie : getContainerContext().getCookies()) {
			if ((name + "").equals(cookie.getName())) {
				String value = cookie.getValue();
				if (value == null) return "";
				return value;
			}
		}
		return "";
	}

	private ContainerContext getContainerContext() {
		return (ContainerContext) application.getContextProperty(
				ContainerContext.CONTEXT_PROPERTY_NAME
				);
	}

	/**
	 * Returns the AceWiki engine.
	 *
	 * @return The AceWiki engine.
	 */
	public AceWikiEngine getEngine() {
		return engine;
	}

	/**
	 * Returns the language of this wiki instance.
	 * 
	 * @return The name of the language.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Returns the locale of this wiki instance.
	 * 
	 * @return The locale.
	 */
	public Locale getLocale() {
		return getLanguageHandler().getLocale();
		// for locale testing:
		//return new Locale("de", "DE");
	}

	/**
	 * Switches to another language.
	 * 
	 * @param language The new language.
	 */
	public void switchLanguage(String language) {
		this.language = language;
		String[] languages = engine.getLanguages();
		for (int i = 0 ; i < languages.length ; i++) {
			String l = languages[i];
			languageButtons.get(i).setEnabled(!l.equals(language));
		}
		application.setLocale(getLocale());
		buildContentPane();
		update();
		refresh();
	}

	/**
	 * Returns the language handler.
	 *
	 * @return The language handler.
	 */
	public LanguageHandler getLanguageHandler() {
		return engine.getLanguageHandler(language);
	}

	/**
	 * Returns whether the wiki is multilingual, i.e. has more than one language.
	 * 
	 * @return true if multilingual.
	 */
	public boolean isMultilingual() {
		return engine.getLanguages().length > 1;
	}

	/**
	 * Returns the logger object.
	 *
	 * @return The logger object.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Runs the task without showing a wait window while it is executed.
	 *
	 * @param task The task.
	 */
	public void enqueueTask(Runnable task) {
		application.enqueueTask(taskQueue, task);
	}

	/**
	 * Runs the task in an asynchronous way and shows a wait window while it is executed. The task
	 * is treated as a strong task that takes precedence over weak tasks.
	 *
	 * @param title The title of the wait window.
	 * @param message The message of the wait window.
	 * @param task The task.
	 */
	public void enqueueStrongAsyncTask(String title, String message, Task task) {
		waitWindow = new MessageWindow(
				title,
				new ResourceImageReference("ch/uzh/ifi/attempto/acewiki/gui/img/wait.gif"),
				message,
				null,
				null
				);
		waitWindow.setClosable(false);
		showWindow(waitWindow);

		strongTasks.add(task);
	}

	/**
	 * Runs the task in an asynchronous way without showing a wait window. The task is treated as a
	 * weak task that can be overtaken by strong tasks.
	 *
	 * @param task The task.
	 */
	public void enqueueWeakAsyncTask(Task task) {
		weakTasks.add(task);
	}

	/**
	 * Returns information about AceWiki, like the version number and the release date. This
	 * information is read from the file "acewiki.properties".
	 *
	 * @param key The key string.
	 * @return The value for the given key.
	 */
	public static String getInfo(String key) {
		if (properties == null) {
			String f = "ch/uzh/ifi/attempto/acewiki/acewiki.properties";
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(f);
			properties = new Properties();
			try {
				properties.load(in);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return properties.getProperty(key);
	}

	/**
	 * Cleans up when the object is no longer used.
	 */
	public void dispose() {
		disposed = true;
		externalEventMonitor.removeExternalEventListener(this);
		externalEventMonitor.dispose();
	}

	/**
	 * This methods locks the general buttons of the wiki interface. When one of these buttons
	 * is pressed, the locked-listener is called.
	 *
	 * @param lockedListener The listener to be called when one of the buttons is pressed.
	 */
	public void lock(ActionListener lockedListener) {
		if (locked) return;
		locked = true;
		this.lockedListener = lockedListener;
	}

	/**
	 * Unlocks the wiki interface, if it has been locked before.
	 */
	public void unlock() {
		locked = false;
	}

	/**
	 * Returns an image reference for a file in the AceWiki image directory with the given file
	 * name.
	 *
	 * @param fileName The name of the image file.
	 * @return The image reference.
	 */
	public static ResourceImageReference getImage(String fileName) {
		return Style.getImage("ch/uzh/ifi/attempto/acewiki/gui/img/" + fileName);
	}

	/**
	 * Returns the GUI text for the current locale.
	 * 
	 * @param key The key of the GUI text item.
	 * @return The localized string.
	 */
	public String getGUIText(String key) {
		String text = LocaleResources.getString(key);
		if (text == null) text = key;
		return text;
	}

	private String getURLParameterValue(String name) {
		String v = null;
		try {
			ContainerContext cc = getContainerContext();
			String[] values = (String[]) cc.getInitialRequestParameterMap().get(name);
			v = values[0];
		} catch (Exception ex) {}
		return v;
	}

}
