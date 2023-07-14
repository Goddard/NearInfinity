// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.infinity.AppOption;
import org.infinity.NearInfinity;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.PreferencesDialog;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.CharsetDetector;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.tuples.Couple;

/**
 * Handles Option menu items for the {@link BrowserMenuBar}.
 */
public class OptionsMenuItem extends JMenuItem implements ActionListener {
  // Symbolic name for the default character set
  private static final String DEFAULT_CHARSET = "Auto";

  // Identifier for autodetected game language
  private static final String LANGUAGE_AUTODETECT = "Auto";

  // List of predefined font sizes (-1 indicates a custom value)
  private static final int[] FONT_SIZES = { 50, 75, 100, 125, 150, 175, 200, 250, 300, 400, -1 };

  // List of predefined ui scaling factors (-1 indicates a custom value)
  private static final int[] UI_SCALING = { 100, 125, 133, 150, 175, 200, 250, 300, 400, -1 };

  /** BCS indentations to use when decompiling (indent, title). */
  private static final List<IndentInfo> BCS_INDENT = Arrays.asList(
      new IndentInfo("2 Spaces", "  "),
      new IndentInfo("4 Spaces", "    "),
      new IndentInfo("Tab", "\t")
  );

  /** Available color schemes for highlighted BCS format (title and scheme definition path). */
  private static final List<ColorScheme> BCS_COLOR_SCHEME = Arrays.asList(
      new ColorScheme(InfinityTextArea.Scheme.DEFAULT.getLabel(), InfinityTextArea.Scheme.DEFAULT.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.DARK.getLabel(), InfinityTextArea.Scheme.DARK.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.DRUID.getLabel(), InfinityTextArea.Scheme.DRUID.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.ECLIPSE.getLabel(), InfinityTextArea.Scheme.ECLIPSE.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.IDEA.getLabel(), InfinityTextArea.Scheme.IDEA.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.MONOKAI.getLabel(), InfinityTextArea.Scheme.MONOKAI.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.VS.getLabel(), InfinityTextArea.Scheme.VS.getScheme()),
      new ColorScheme(InfinityTextArea.Scheme.BCS.getLabel(), InfinityTextArea.Scheme.BCS.getScheme())
  );

  /** Available color schemes for remaining highlighted formats (scheme, title, description). */
  private static final List<ColorScheme> COLOR_SCHEME = BCS_COLOR_SCHEME.subList(0, BCS_COLOR_SCHEME.size() - 1);

  // List of predefined script text fonts
  private static final List<Font> FONTS = Arrays.asList(
      new Font(Font.MONOSPACED, Font.PLAIN, 12),
      new Font(Font.SERIF, Font.PLAIN, 12),
      new Font(Font.SANS_SERIF, Font.PLAIN, 12),
      new Font(Font.DIALOG, Font.PLAIN, 12),
      null  // custom font selection
  );

  // List of predefined character sets (canonical name and display name)
  private static final List<CharsetInfo> CHARSETS_USED = Arrays.asList(
      new CharsetInfo("UTF-8", "UTF-8"),
      new CharsetInfo("windows-1252", "Windows-1252"),
      new CharsetInfo("windows-1251", "Windows-1251"),
      new CharsetInfo("windows-1250", "Windows-1250"),
      new CharsetInfo("windows-31j", "Windows-31J"),
      new CharsetInfo("GBK", "GBK"),
      new CharsetInfo("Big5-HKSCS", "Big5-HKSCS"),
      new CharsetInfo("x-IBM949", "IBM-949")
  );

  public static final String OPTION_SHOWOFFSETS               = "ShowOffsets";
  public static final String OPTION_SHOWOFFSETSRELATIVE       = "ShowOffsetsRelative";
  public static final String OPTION_SHOWSIZE                  = "ShowSize";
  public static final String OPTION_SHOWSIZEHEX               = "ShowSizeHex";
  public static final String OPTION_BACKUPONSAVE              = "BackupOnSave";
  public static final String OPTION_IGNOREOVERRIDE            = "IgnoreOverride";
  public static final String OPTION_IGNOREREADERRORS          = "IgnoreReadErrors";
  public static final String OPTION_LAUNCHGAMEALLOWED         = "LaunchGameAllowed";
  public static final String OPTION_SHOWUNKNOWNRESOURCES      = "ShowUnknownResources";
  public static final String OPTION_SHOWTREESEARCHNAMES       = "ShowTreeSearchNames";
  public static final String OPTION_SHOW_RESOURCE_LIST_ICONS  = "ShowResourceListIcons";
  public static final String OPTION_SHOW_RESOURCE_TREE_ICONS  = "ShowResourceTreeIcons";
  public static final String OPTION_HIGHLIGHT_OVERRIDDEN      = "HighlightOverridden";
  public static final String OPTION_CACHEOVERRIDE             = "CacheOverride";
  public static final String OPTION_KEEPVIEWONCOPY            = "UpdateTreeOnCopy";
  public static final String OPTION_SHOWSTRREFS               = "ShowStrrefs";
  public static final String OPTION_SHOWCOLOREDSTRUCTURES     = "ShowColoredStructures";
  public static final String OPTION_SHOWHEXCOLORED            = "ShowHexColored";
  public static final String OPTION_SHOWSYSINFO               = "ShowSysInfo";
  public static final String OPTION_OPENBOOKMARKSPROMPT       = "OpenBookmarksPrompt";
  public static final String OPTION_REMEMBER_CHILDFRAME_RECT  = "RememberChildFrameRect";

  public static final String OPTION_AUTOCHECK_BCS             = "AutocheckBCS";
  public static final String OPTION_AUTOGEN_BCS_COMMENTS      = "AutogenBCSComments";
  public static final String OPTION_MORECOMPILERWARNINGS      = "MoreCompilerWarnings";
//  public static final String OPTION_MONITORFILECHANGES        = "MonitorFileChanges";

  public static final String OPTION_TEXT_SHOWCURRENTLINE      = "TextShowCurrentLine";
  public static final String OPTION_TEXT_SHOWLINENUMBERS      = "TextShowLineNumbers";
  public static final String OPTION_TEXT_SYMBOLWHITESPACE     = "TextShowWhiteSpace";
  public static final String OPTION_TEXT_SYMBOLEOL            = "TextShowEOL";
  public static final String OPTION_TEXT_TABSEMULATED         = "TextTabsEmulated";
  public static final String OPTION_TEXT_TABSIZE              = "TextTabSize";
  public static final String OPTION_BCS_SYNTAXHIGHLIGHTING    = "BcsSyntaxHighlighting";
  public static final String OPTION_BCS_COLORSCHEME           = "BcsColorScheme";
  public static final String OPTION_BCS_CODEFOLDING           = "BcsCodeFolding";
  public static final String OPTION_BCS_AUTO_INDENT           = "BcsAutoIndent";
  public static final String OPTION_BCS_INDENT                = "BcsIndent";
  public static final String OPTION_GLSL_SYNTAXHIGHLIGHTING   = "GlslSyntaxHighlighting";
  public static final String OPTION_GLSL_COLORSCHEME          = "GlslColorScheme";
  public static final String OPTION_GLSL_CODEFOLDING          = "GlslCodeFolding";
  public static final String OPTION_INI_SYNTAXHIGHLIGHTING    = "IniSyntaxHighlighting";
  public static final String OPTION_INI_COLORSCHEME           = "IniColorScheme";
  public static final String OPTION_LUA_SYNTAXHIGHLIGHTING    = "LuaSyntaxHighlighting";
  public static final String OPTION_LUA_COLORSCHEME           = "LuaColorScheme";
  public static final String OPTION_SQL_SYNTAXHIGHLIGHTING    = "SqlSyntaxHighlighting";
  public static final String OPTION_SQL_COLORSCHEME           = "SqlColorScheme";
  public static final String OPTION_TLK_SYNTAXHIGHLIGHTING    = "TlkSyntaxHighlighting";
  public static final String OPTION_TLK_COLORSCHEME           = "TlkColorScheme";
  public static final String OPTION_WEIDU_SYNTAXHIGHLIGHTING  = "WeiDUSyntaxHighlighting";
  public static final String OPTION_WEIDU_COLORSCHEME         = "WeiDUColorScheme";

  public static final String OPTION_SHOWICONS                 = "DlgShowIcons";
  public static final String OPTION_SORT_STATES_BY_WEIGHT     = "DlgSortStatesByWeight";
  public static final String OPTION_ALWAYS_SHOW_STATE_0       = "DlgAlwaysShowState0";
  public static final String OPTION_COLORIZE_OTHER_DIALOGS    = "DlgColorizeOtherDialogs";
  public static final String OPTION_BREAK_CYCLES              = "DlgBreakCycles";
  public static final String OPTION_COLORIZE_RESPONSES        = "DlgColorizeResponses";
  public static final String OPTION_SHOW_TECH_INFO            = "DlgShowTechInfo";

  public static final String OPTION_SHOWOVERRIDES             = "ShowOverridesIn";
  public static final String OPTION_SHOWRESREF                = "ShowResRef";
  public static final String OPTION_LOOKANDFEELCLASS          = "LookAndFeelClass";
  public static final String OPTION_VIEWOREDITSHOWN           = "ViewOrEditShown";
  public static final String OPTION_FONT                      = "Font";
  public static final String OPTION_FONT_NAME                 = "FontName";
  public static final String OPTION_FONT_STYLE                = "FontStyle";
  public static final String OPTION_FONT_SIZE                 = "FontSize";
  public static final String OPTION_TLKCHARSET                = "TLKCharsetType";
  public static final String OPTION_LANGUAGE_GAMES            = "GameLanguages";

  /** This preferences key can be used internally to reset incorrectly set default values after a public release. */
  public static final String OPTION_OPTION_FIXED             = "OptionFixedInternal";

  /** Mask used for one-time resets of options (kept track of in OPTION_OPTION_FIXED). */
  /** Bit for incorrect BCS Auto Indent default: {@code false} -> {@code true}. */
  @Deprecated
  public static final int MASK_OPTION_FIXED_AUTO_INDENT = 0x00000001;

  private boolean launchGameEnabled;

  /** Returns a list of all available BCS indentation types. */
  public static List<IndentInfo> getIndentations() {
    return Collections.unmodifiableList(BCS_INDENT);
  }

  /** Returns a list of all available color schemes for BCS format. */
  public static List<ColorScheme> getBcsColorSchemes() {
    return Collections.unmodifiableList(BCS_COLOR_SCHEME);
  }

  /** Returns a list of all available color schemes for text-based formats except BCS. */
  public static List<ColorScheme> getColorSchemes() {
    return Collections.unmodifiableList(COLOR_SCHEME);
  }

  /** Returns a copy of available font sizes for the "Global Font Size" option. */
  public static int[] getFontSizes() {
    return Arrays.copyOf(FONT_SIZES, FONT_SIZES.length);
  }

  /** Returns a copy of available UI scale factors for the "Override UI Scaling" option. */
  public static int[] getUiScalingFactors() {
    return Arrays.copyOf(UI_SCALING, UI_SCALING.length);
  }

  /** Returns a list of available fonts. {@code null} indicates to use a user-defined font. */
  public static List<Font> getFonts() {
    return Collections.unmodifiableList(FONTS);
  }

  /** Returns the symbolic name of the default charset. */
  public static String getDefaultCharset() {
    return DEFAULT_CHARSET;
  }

  /** Returns a list of default character sets for dialog.tlk strings. */
  public static List<CharsetInfo> getDefaultCharsets() {
    return Collections.unmodifiableList(CHARSETS_USED);
  }

  /** Returns the name of the language specified by the given language code. */
  public static String getDisplayLanguage(String langCode) {
    String retVal = langCode;
    String[] lang = langCode.split("_");
    if (lang.length >= 2) {
      retVal = (new Locale(lang[0], lang[1])).getDisplayLanguage();
      if (retVal == null || retVal.isEmpty()) {
        retVal = langCode;
      }
    }
    return retVal;
  }

  /** Returns the symbolic name of the default game language for EE games. */
  public static String getDefaultGameLanguage() {
    return LANGUAGE_AUTODETECT;
  }

  /** Returns the language definition stored in "definition" for the specified game. */
  public static String getGameLanguage(String definition, Profile.Game game) {
    if (game != null && game != Profile.Game.Unknown) {
      List<Couple<String, String>> list = extractGameLanguages(definition);
      for (Couple<String, String> pair : list) {
        Profile.Game curGame = Profile.gameFromString(pair.getValue0());
        if (curGame == game) {
          return pair.getValue1();
        }
      }
    }
    return LANGUAGE_AUTODETECT;
  }

  /** Adds or updates the Game/Language pair in the formatted "definition" string. */
  public static String updateGameLanguages(String definition, Profile.Game game, String langCode) {
    if (game != null && langCode != null) {
      List<Couple<String, String>> list = extractGameLanguages(definition);

      // attempt to update existing entry first
      for (final Couple<String, String> curPair : list) {
        if (curPair.getValue0().equalsIgnoreCase(game.toString())) {
          curPair.setValue1(langCode);
          return createGameLanguages(list);
        }
      }

      // add new entry if necessary
      list.add(Couple.with(game.toString(), langCode));
      return createGameLanguages(list);
    }
    return "";
  }

  /** Extracts entries of Game/Language pairs from the given argument. */
  private static List<Couple<String, String>> extractGameLanguages(String definition) {
    List<Couple<String, String>> list = new ArrayList<>();
    if (definition != null && !definition.isEmpty()) {
      String[] entries = definition.split(";");
      if (entries != null) {
        for (final String entry : entries) {
          String[] elements = entry.split("=");
          if (elements != null && elements.length == 2) {
            Profile.Game game = Profile.gameFromString(elements[0]);
            if (game != Profile.Game.Unknown) {
              String lang = elements[1].trim();
              Couple<String, String> pair = null;
              if (lang.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
                pair = Couple.with(game.toString(), LANGUAGE_AUTODETECT);
              } else if (lang.matches("[a-z]{2}_[A-Z]{2}")) {
                pair = Couple.with(game.toString(), lang);
              }

              // check if game/language pair is already in the list
              if (pair != null) {
                for (final Couple<String, String> curPair : list) {
                  if (curPair.getValue0().equalsIgnoreCase(pair.getValue0())) {
                    curPair.setValue1(pair.getValue1());
                    pair = null;
                    break;
                  }
                }
              }

              if (pair != null) {
                list.add(pair);
              }
            }
          }
        }
      }
    }
    return list;
  }

  /** Creates a formatted string out of the Game/Language pairs included in the given list. */
  private static String createGameLanguages(List<Couple<String, String>> list) {
    StringBuilder sb = new StringBuilder();
    if (list != null) {
      for (final Iterator<Couple<String, String>> iter = list.iterator(); iter.hasNext();) {
        Couple<String, String> pair = iter.next();
        sb.append(String.format("%s=%s", pair.getValue0(), pair.getValue1()));
        if (iter.hasNext()) {
          sb.append(';');
        }
      }
    }
    return sb.toString();
  }

  public OptionsMenuItem() {
    super("Preferences...");
    setMnemonic(KeyEvent.VK_P);
    // TODO: improve icon
    setIcon(Icons.ICON_APPLICATION_16.getIcon());
    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, BrowserMenuBar.getCtrlMask()));

    launchGameEnabled = true;
  }

  /**
   * Shows the Preferences dialog for the user to make changes.
   *
   * @param owner  the {@code Window} from which the dialog is displayed or {@code null} if this dialog has no owner.
   * @return {@code true} if the user accepted any changes made in the dialog. {@code false} if the user discarded
   * the changes, either by clicking the "Cancel" button or closing the dialog.
   */
  public boolean showPreferencesDialog(Window owner) {
    if (PreferencesDialog.showDialog(owner)) {
      if (AppOption.isAnyModified()) {
        applyChanges(AppOption.getModifiedOptions());
      }
      return true;
    }
    return false;
  }

  /** Attempts to determine the correct charset for the current game. */
  public String charsetName(String charset, boolean detect) {
    if (DEFAULT_CHARSET.equalsIgnoreCase(charset)) {
      charset = CharsetDetector.guessCharset(detect);
    } else {
      charset = CharsetDetector.setCharset(charset);
    }
    return charset;
  }

  public void gameLoaded() {
    // update charset selection
    StringTable.setCharset(getSelectedCharset());
  }

  public boolean isLaunchGameMenuEnabled() {
    return launchGameEnabled;
  }

  /** Specifies the enabled state of the "Allow launching games" entry in the Preferences dialog. */
  public void setLaunchGameMenuEnabled(boolean enable) {
    launchGameEnabled = enable;
  }

  /** Returns whether a backup is created when resources are modified. */
  public boolean backupOnSave() {
    return AppOption.BACKUP_ON_SAVE.getBoolValue();
  }

  /** Returns whether launching game executables in NI is enabled. */
  public boolean getLauncherEnabled() {
    return AppOption.LAUNCH_GAME_ALLOWED.getBoolValue();
  }

  /** Returns whether override files are ignored in the resource tree. */
  public boolean ignoreOverrides() {
    return AppOption.IGNORE_OVERRIDE.getBoolValue();
  }

  /** Returns if read errors are shown in the status bar instead of a dialog prompt. */
  public boolean ignoreReadErrors() {
    return AppOption.IGNORE_READ_ERRORS.getBoolValue();
  }

  /** Returns whether unknown or unrecognized resources are displayed in the resource tree. */
  public boolean showUnknownResourceTypes() {
    return AppOption.SHOW_UNKNOWN_RESOURCES.getBoolValue();
  }

  /** Returns whether search names are displayed alongside resource names in the resource tree. */
  public boolean showTreeSearchNames() {
    return AppOption.SHOW_TREE_SEARCH_NAMES.getBoolValue();
  }

  /** Returns whether icons are shown alongside resource names for ITM and SPL resources in resource selection lists. */
  public boolean showResourceListIcons() {
    return AppOption.SHOW_RESOURCE_LIST_ICONS.getBoolValue();
  }

  /** Returns whether icons are shown alongside resource names for ITM and SPL resources in the resource tree. */
  public boolean showResourceTreeIcons() {
    return AppOption.SHOW_RESOURCE_TREE_ICONS.getBoolValue();
  }

  /** Returns whether overridden files are displayed in bold in the resource tree. */
  public boolean highlightOverridden() {
    return AppOption.HIGHLIGHT_OVERRIDDEN.getBoolValue();
  }

  /** Returns whether the "Autocheck for Overrides" option is enabled. */
  public boolean cacheOverride() {
    return AppOption.CACHE_OVERRIDE.getBoolValue();
  }

  /** Returns whether the "Add copy of" operation keeps the original resource selected. */
  public boolean getKeepViewOnCopy() {
    return AppOption.KEEP_VIEW_ON_COPY.getBoolValue();
  }

  /** Returns whether string references are shown alongside strings. */
  public boolean showStrrefs() {
    return AppOption.SHOW_STRREFS.getBoolValue();
  }

  /** Returns whether substructures and their related offset and count fields are colorized for structured resources. */
  public boolean getColoredOffsetsEnabled() {
    return AppOption.SHOW_COLORED_STRUCTURES.getBoolValue();
  }

  /** Returns whether substructures are colorized in the raw tab of structured resources. */
  public boolean getHexColorMapEnabled() {
    return AppOption.SHOW_HEX_COLORED.getBoolValue();
  }

  /** Returns whether system information are shown when NI starts up. */
  public boolean showSysInfo() {
    return AppOption.SHOW_SYS_INFO.getBoolValue();
  }

  /** Returns whether to show a dialog prompt whenever a bookmarked game is opened. */
  public boolean showOpenBookmarksPrompt() {
    return AppOption.OPEN_BOOKMARKS_PROMPT.getBoolValue();
  }

  /**
   * Controls whether to show a dialog prompt whenever a bookmarked game is opened.
   *
   * @param show {@code true} to show a dialog prompt, {@code false} to open bookmarked games without confirmation.
   */
  public void setShowOpenBookmarksPrompt(boolean show) {
    AppOption.OPEN_BOOKMARKS_PROMPT.setInitialValue(show);
  }

  /** Returns whether the size and position of the last opened child frame should be reused for new child frames. */
  public boolean rememberChildFrameRect() {
    return AppOption.REMEMBER_CHILD_FRAME_RECT.getBoolValue();
  }

  /** Returns whether offset column is shown for structured resources. */
  public boolean showTableOffsets() {
    return AppOption.TABLE_SHOW_OFFSETS.getBoolValue();
  }

  /** Returns whether relative offsets are shown for fields in substructures. */
  public boolean showTableOffsetsRelative() {
    return AppOption.TABLE_SHOW_OFFSETS_RELATIVE.getBoolValue();
  }

  /** Returns whether size column is shown for structured resources. */
  public boolean showTableSize() {
    return AppOption.TABLE_SHOW_SIZE.getBoolValue();
  }

  /** Returns whether size column is shown in hex (or decimal) for structured resources. */
  public boolean showTableSizeInHex() {
    return AppOption.TABLE_SHOW_SIZE_HEX.getBoolValue();
  }

  /** Returns whether scripts are automatically scanned for compile errors. */
  public boolean autocheckBCS() {
    return AppOption.AUTO_CHECK_BCS.getBoolValue();
  }

  /** Returns whether extended compiler warnings are shown for scripts. */
  public boolean showMoreCompileWarnings() {
    return AppOption.MORE_COMPILER_WARNINGS.getBoolValue();
  }

  /** Returns whether helpful comments are generated for decompiled scripts. */
  public boolean autogenBCSComments() {
    return AppOption.AUTO_GEN_BCS_COMMENTS.getBoolValue();
  }

  /** Returns state of "Text: Show Whitespace and Tab" */
  public boolean getTextWhitespaceVisible() {
    return AppOption.TEXT_SHOW_WHITE_SPACE.getBoolValue();
  }

  /** Returns state of "Text: Show End of Line" */
  public boolean getTextEOLVisible() {
    return AppOption.TEXT_SHOW_EOL.getBoolValue();
  }

  /** Returns state of "Text: Show Highlight Current Line" */
  public boolean getTextHighlightCurrentLine() {
    return AppOption.TEXT_SHOW_CURRENT_LINE.getBoolValue();
  }

  /** Returns state of "Text: Show Line Numbers" */
  public boolean getTextLineNumbers() {
    return AppOption.TEXT_SHOW_LINE_NUMBERS.getBoolValue();
  }

  /** Returns whether to emulate tabs by inserting spaces instead. */
  public boolean isTextTabEmulated() {
    return AppOption.TEXT_TABS_EMULATED.getBoolValue();
  }

  /** Returns the number of spaces used for (real or emulated) tabs. */
  public int getTextTabSize() {
    return 1 << (getTextIndentIndex() + 1);
  }

  /** Returns the array index of the selected tab size. */
  public int getTextIndentIndex() {
    return AppOption.TEXT_TAB_SIZE.getIntValue();
  }

  /** Returns state of "BCS: Enable Automatic Indentation" */
  public boolean getBcsAutoIndentEnabled() {
    return AppOption.BCS_AUTO_INDENT.getBoolValue();
  }

  /** Returns the selected indentation for BCS resources as literal string. */
  public String getBcsIndent() {
    int idx = AppOption.BCS_INDENT.getIntValue();
    return BCS_INDENT.get(idx).getIndentation();
  }

  /** Returns state of "BCS: Enable Syntax Highlighting" */
  public boolean getBcsSyntaxHighlightingEnabled() {
    return AppOption.BCS_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for GLSL" */
  public boolean getGlslSyntaxHighlightingEnabled() {
    return AppOption.GLSL_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for INI" */
  public boolean getIniSyntaxHighlightingEnabled() {
    return AppOption.INI_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for LUA" */
  public boolean getLuaSyntaxHighlightingEnabled() {
    return AppOption.LUA_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for SQL" */
  public boolean getSqlSyntaxHighlightingEnabled() {
    return AppOption.SQL_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for TLK" */
  public boolean getTlkSyntaxHighlightingEnabled() {
    return AppOption.TLK_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "Enable Syntax Highlighting for WeiDU.log" */
  public boolean getWeiDUSyntaxHighlightingEnabled() {
    return AppOption.WEIDU_SYNTAX_HIGHLIGHTING.getBoolValue();
  }

  /** Returns state of "BCS: Enable Code Folding" */
  public boolean getBcsCodeFoldingEnabled() {
    return AppOption.BCS_CODE_FOLDING.getBoolValue();
  }

  /** Returns state of "Enable Code Folding for GLSL" */
  public boolean getGlslCodeFoldingEnabled() {
    return AppOption.GLSL_CODE_FOLDING.getBoolValue();
  }

  public String getBcsColorScheme() {
    int idx = AppOption.BCS_COLOR_SCHEME.getIntValue();
    return BCS_COLOR_SCHEME.get(idx).getPath();
  }

  public String getGlslColorScheme() {
    int idx = AppOption.GLSL_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  public String getIniColorScheme() {
    int idx = AppOption.INI_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  public String getLuaColorScheme() {
    int idx = AppOption.LUA_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  public String getSqlColorScheme() {
    int idx = AppOption.SQL_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  public String getTlkColorScheme() {
    int idx = AppOption.TLK_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  public String getWeiDUColorScheme() {
    int idx = AppOption.WEIDU_COLOR_SCHEME.getIntValue();
    return COLOR_SCHEME.get(idx).getPath();
  }

  /** Returns whether the dialog tree viewer shows icons in front of state and response entries. */
  public boolean showDlgTreeIcons() {
    return AppOption.DLG_SHOW_ICONS.getBoolValue();
  }

  /** Returns whether root states are sorted by processing order (based on state trigger index). */
  public boolean sortStatesByWeight() {
    return AppOption.DLG_SORT_STATES_BY_WEIGHT.getBoolValue();
  }

  /** Returns whether state 0 is always shown as a root node in the dialog tree viewer. */
  public boolean alwaysShowState0() {
    return AppOption.DLG_ALWAYS_SHOW_STATE_0.getBoolValue();
  }

  /** Returns whether external dialog references are shown with a colored background in the dialog tree viewer. */
  public boolean colorizeOtherDialogs() {
    return AppOption.DLG_COLORIZE_OTHER_DIALOGS.getBoolValue();
  }

  /**
   * Returns whether duplicate states are combined and only shown once to break infinite loops in the dialog tree
   * viewer.
   */
  public boolean breakCyclesInDialogs() {
    return AppOption.DLG_BREAK_CYCLES.getBoolValue();
  }

  /** Returns whether response entries in the dialog tree viewer are shown with a colored background. */
  public boolean useDifferentColorForResponses() {
    return AppOption.DLG_COLORIZE_RESPONSES.getBoolValue();
  }

  /** Returns whether additional information about the dialog is shown in the dialog tree viewer. */
  public boolean showDlgTechInfo() {
    return AppOption.DLG_SHOW_TECH_INFO.getBoolValue();
  }

  public ResRefMode getResRefMode() {
    final int mode = AppOption.SHOW_RES_REF.getIntValue();
    return Arrays
        .stream(ResRefMode.values())
        .filter(m -> m.ordinal() == mode)
        .findAny()
        .orElse(ResRefMode.RefName);
  }

  public OverrideMode getOverrideMode() {
    final int mode = AppOption.SHOW_OVERRIDES_IN.getIntValue();
    return Arrays
        .stream(OverrideMode.values())
        .filter(m -> m.ordinal() == mode)
        .findAny()
        .orElse(OverrideMode.Split);
  }

  public ViewMode getDefaultStructView() {
    final int mode = AppOption.VIEW_OR_EDIT_SHOWN.getIntValue();
    return Arrays
        .stream(ViewMode.values())
        .filter(m -> m.ordinal() == mode)
        .findAny()
        .orElse(ViewMode.Edit);
  }

  /** Returns whether Global UI Scaling is enabled. */
  public boolean isUiScalingEnabled() {
    return AppOption.UI_SCALE_ENABLED.getBoolValue();
  }

  /** Returns the selected scaling factor for the Global UI Scaling option. */
  public int getUiScalingFactor() {
    return AppOption.UI_SCALE_FACTOR.getIntValue();
  }

  /** Returns the selected scaling factor for the Global Font Size option. */
  public int getGlobalFontSize() {
    return AppOption.GLOBAL_FONT_SIZE.getIntValue();
  }

  /** Returns the selected Look&Feel theme. */
  public LookAndFeelInfo getLookAndFeel() {
    String value = AppOption.LOOK_AND_FEEL_CLASS.getStringValue();
    LookAndFeelInfo info = null;
    try {
      if (Class.forName(value) != null) {
        info = new LookAndFeelInfo(AppOption.LOOK_AND_FEEL_CLASS.getName(), value);
      }
    } catch (Exception e) {
    }

    if (info == null) {
      info = BrowserMenuBar.getDefaultLookAndFeel();
    }

    return info;
  }

  /** Returns the selected font for scripts. */
  public Font getScriptFont() {
    int fontIndex = AppOption.TEXT_FONT.getIntValue();

    final Font font;
    if (fontIndex < FONTS.size() - 1) {
      // predefined font
      font = FONTS.get(fontIndex);
    } else {
      // custom font
      String name = AppOption.TEXT_FONT_NAME.getStringValue();
      int style = AppOption.TEXT_FONT_STYLE.getIntValue();
      int size = AppOption.TEXT_FONT_SIZE.getIntValue();
      font = new Font(name, style, size);
    }

    return font;
  }

  /** Determines whether the specified character set exists. */
  public boolean isCharsetAvailable(String charset) {
    if (charset != null && !charset.isEmpty()) {
      if (DEFAULT_CHARSET.equalsIgnoreCase(charset)) {
        return true;
      }
      try {
        return (Charset.forName(charset) != null);
      } catch (Throwable t) {
        return false;
      }
    }
    return false;
  }

  /** Returns the character encoding of the string table. */
  public String getSelectedCharset() {
    return charsetName(AppOption.TLK_CHARSET_TYPE.getStringValue(), true);
  }

  /** Returns the currently selected game language. Returns empty string on autodetect. */
  public String getSelectedGameLanguage() {
    final String languages = AppOption.GAME_LANGUAGES.getStringValue();
    String lang = getGameLanguage(languages, Profile.getGame());
    return lang.equalsIgnoreCase(LANGUAGE_AUTODETECT) ? "" : lang;
  }

  /**
   * Returns whether file changes in override folders are tracked at real time and reflected in the resource tree.
   *
   * (not yet implemented)
   */
  public boolean getMonitorFileChanges() {
//      return optionMonitorFileChanges.isSelected();
    return false;
  }

  /** Returns defValue if masked bit is clear or value if masked bit is already set. */
  public boolean fixOption(int mask, boolean defValue, boolean value) {
    boolean retVal = value;
    int fixedInternal = AppOption.OPTION_FIXED_INTERNAL.getIntValue();
    if ((fixedInternal & mask) == 0) {
      retVal = defValue;
      fixedInternal |= mask;
      AppOption.OPTION_FIXED_INTERNAL.setInitialValue(fixedInternal);
    }
    return retVal;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    // if (event.getSource() == optionMonitorFileChanges) {
    //   if (optionMonitorFileChanges.isSelected()) {
    //     FileWatcher.getInstance().start();
    //   } else {
    //     FileWatcher.getInstance().stop();
    //   }
    // }
  }

  /** Attempts to switch the game language in Enhanced Edition games. */
  private void switchGameLanguage(String newLanguage, boolean prompt) {
    if (newLanguage != null) {
      // switch language and refresh resources
      String oldLanguage = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAME);
      String oldLangName = getDisplayLanguage(oldLanguage);
      String newLanguageCode;
      if (newLanguage.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
        // "Autodetect" must be converted into an actual language code before proceeding
        newLanguageCode = ResourceFactory.autodetectGameLanguage(Profile.getProperty(Profile.Key.GET_GAME_INI_FILE));
      } else {
        newLanguageCode = newLanguage;
      }
      String newLangName = getDisplayLanguage(newLanguageCode);
      boolean success = false, showErrorMsg = false;
      if (!prompt ||
          JOptionPane.showConfirmDialog(NearInfinity.getInstance(),
              String.format("Do you want to switch from \"%s\" to \"%s\"?", oldLangName, newLangName),
              "Switch game language", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        if (Profile.updateGameLanguage(newLanguageCode)) {
          String languageDefinition = AppOption.GAME_LANGUAGES.getStringValue();
          updateGameLanguages(languageDefinition, Profile.getGame(), newLanguage);
          NearInfinity.getInstance().refreshGame();
          success = true;
        } else {
          showErrorMsg = true;
        }
      }
      if (!success) {
        if (showErrorMsg) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Unable to set new language.", "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  /** Evaluates the specified options and applies modifications to the current session if needed. */
  private void applyChanges(Collection<AppOption> options) {
    final List<String> messages = new ArrayList<>();
    boolean restart = false;
    boolean refresh = false;

    // applying options
    for (final AppOption option : options) {
      if (option.isModified()) {
        if (option.equals(AppOption.SHOW_UNKNOWN_RESOURCES) ||
            option.equals(AppOption.SHOW_OVERRIDES_IN) ||
            option.equals(AppOption.SHOW_RESOURCE_TREE_ICONS)) {
          refresh = true;
          messages.add(String.format("%s: %s", option.getLabel(), option.getValue()));
        } else if (option.equals(AppOption.UI_SCALE_ENABLED) ||
            option.equals(AppOption.UI_SCALE_FACTOR) ||
            option.equals(AppOption.GLOBAL_FONT_SIZE)) {
          restart = true;
          messages.add(String.format("%s: %s", option.getLabel(), option.getValue()));
        } else if (option.equals(AppOption.LAUNCH_GAME_ALLOWED)) {
          NearInfinity.getInstance().updateLauncher();
          messages.add(String.format("Allow launching games: %s", option.getValue()));
        } else if (option.equals(AppOption.TLK_CHARSET_TYPE)) {
          final String csName = option.getStringValue();
          if (csName != null) {
            CharsetDetector.clearCache();
            StringTable.setCharset(charsetName(csName, true));
            messages.add("TLK Character Encoding: " + csName);
            // enforce re-reading strings
            refresh = true;
          }
        } else if (option.equals(AppOption.GAME_LANGUAGES)) {
          final String languages = option.getStringValue();
          final String language = getGameLanguage(languages, Profile.getGame());
          switchGameLanguage(language, false);
          messages.add("Game language: " + getDisplayLanguage(language));
        } else if (option.equals(AppOption.LOOK_AND_FEEL_CLASS)) {
          try {
            final String className = option.getStringValue();
            String lfName = className;
            try {
              Class<?> cls = Class.forName(className);
              lfName = cls.getSimpleName();
              Object o = cls.getDeclaredConstructor().newInstance();
              if (o instanceof LookAndFeel) {
                lfName = ((LookAndFeel) o).getName();
              }
            } catch (Exception e) {
            }
            final LookAndFeelInfo info = new LookAndFeelInfo(lfName, className);
            NearInfinity.getInstance().updateLookAndFeel(info, false);
            messages.add("Look and Feel UI: " + info.getName());
            refresh = true;
            restart = true;
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else if (option.equals(AppOption.TEXT_FONT)) {
          int idx = option.getIntValue();
          final String fontName;
          final int fontSize;
          if (idx >= 0 && idx < getFonts().size() - 1) {
            fontName = getFonts().get(idx).getName();
            fontSize = getFonts().get(idx).getSize();
          } else {
            fontName = AppOption.TEXT_FONT_NAME.getStringValue();
            fontSize = AppOption.TEXT_FONT_SIZE.getIntValue();
          }
          messages.add(String.format("%s: %s %d", option.getLabel(), fontName, fontSize));
        } else {
          messages.add(String.format("%s: %s", option.getLabel(), option.getValue()));
        }

        // Clearing "modified" flag
        option.setInitialValue(option.getValue());
      }
    }

    if (refresh) {
      NearInfinity.getInstance().refreshGame();
    }

    // Assembling list of modified options
    final StringBuilder sb = new StringBuilder();
    if (!messages.isEmpty()) {
      for (final String s : messages) {
        sb.append("- ").append(s).append('\n');
      }
      // remove trailing line break
      sb.delete(sb.length() - 1, sb.length());
    }

    // trailing message for info dialog
    final String message;
    if (restart) {
      message = "It is recommended to restart Near Infinity for the settings to take full effect.";
    } else {
      message = "Settings have been applied to Near Infinity.";
    }

    if (sb.length() > 0 || message != null) {
      // constructing dialog content pane
      JPanel panel = new JPanel(new BorderLayout(8, 8));

      if (sb.length() > 0) {
        // constructing list of modified options
        JLabel modifiedLabel = new JLabel(String.format("Modified settings (%d):", messages.size()), SwingConstants.LEADING);
        panel.add(modifiedLabel, BorderLayout.NORTH);

        // list of modified options
        JTextArea textArea = new JTextArea(sb.toString());
//        textArea.setBackground(panel.getBackground());
        textArea.setBackground(Misc.getDefaultColor("Label.background", Color.GRAY));
        textArea.setFont(UIManager.getDefaults().getFont("Label.font"));
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane scroller = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scroller, BorderLayout.CENTER);

        // limiting number of visible lines
        final FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int height = Math.min(15, messages.size()) * fm.getHeight() + fm.getHeight() / 2;
        final Dimension dim = scroller.getPreferredSize();
        dim.width += UIManager.getInt("ScrollBar.width"); // prevents cut off text
        dim.height = height;
        scroller.setPreferredSize(dim);
      }

      if (message != null) {
        JTextPane msgPane = new JTextPane();
        StyledDocument style = msgPane.getStyledDocument();
        SimpleAttributeSet align = new SimpleAttributeSet();
        StyleConstants.setAlignment(align, StyleConstants.ALIGN_LEFT);
        style.setParagraphAttributes(0, style.getLength(), align, false);
        msgPane.setBackground(Misc.getDefaultColor("Label.background", Color.GRAY));
        msgPane.setFont(UIManager.getDefaults().getFont("Label.font"));
        msgPane.setEditable(false);
        msgPane.setFocusable(false);
        msgPane.setBorder(BorderFactory.createEmptyBorder());
        msgPane.setText(message);
        panel.add(msgPane, BorderLayout.SOUTH);
      }

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, "Settings changed", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Provides information about line indentation. */
  public static class IndentInfo {
    private final String label;
    private final String indent;

    /**
     * Creates a new indentation object.
     * @param label Short descriptive label for the indentation type.
     * @param indent The literal indentation as string.
     */
    public IndentInfo(String label, String indent) {
      this.label = Objects.requireNonNull(label);
      this.indent = Objects.requireNonNull(indent);
    }

    /** Returns the indentation label. */
    public String getLabel() {
      return label;
    }

    /** Returns the literal indentation string. */
    public String getIndentation() {
      return indent;
    }

    @Override
    public String toString() {
      return getLabel();
    }
  }

  /** Provides information about a syntax highlighting scheme stored within the JAR archive. */
  public static class ColorScheme {
    private final String resourcePath;
    private final String label;

    /**
     * Creates a new syntax highlighting scheme.
     * @param label Short label for menu items or labels.
     * @param path Relative path to the resource definition file within the current JAR archive.
     */
    public ColorScheme(String label, String path) {
      this.resourcePath = Objects.requireNonNull(path);
      this.label = Objects.requireNonNull(label);
    }

    /** Returns the scheme label. */
    public String getLabel() {
      return label;
    }

    /** Returns the scheme resource path. */
    public String getPath() {
      return resourcePath;
    }

    @Override
    public String toString() {
      return getLabel();
    }
  }

  /** Provides information about a character set. */
  public static class CharsetInfo {
    private final String id;
    private final String label;

    private String desc;

    public CharsetInfo(String id, String label) {
      this.id = Objects.requireNonNull(id);
      this.label = Objects.requireNonNull(label);
      init();
    }

    /** Returns the canonical character set name. */
    public String getId() {
      return id;
    }

    /** Returns the given character set display name. */
    public String getLabel() {
      return label;
    }

    /** Returns additional information about the character set (e.g. alias names). */
    public String getDesc() {
      return desc;
    }

    @Override
    public String toString() {
      return getLabel();
    }

    private void init() {
      StringBuilder sb = new StringBuilder();
      Charset cs = Charset.forName(getId());
      if (cs != null && !cs.aliases().isEmpty()) {
        sb.append("Charset aliases: ")
        .append(cs.aliases().stream().collect(Collectors.joining(", ")));
      }
      desc = sb.toString();
    }
  }
}
