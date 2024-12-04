// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.are.viewer.ViewerConstants.LayerStackingType;
import org.infinity.util.SimpleListModel;

/**
 * A settings dialog for the area viewer.
 */
public class SettingsDialog extends JDialog implements ActionListener, ListSelectionListener {
  private static final String[] AnimationFrames = { "Never", "On mouse-over only", "Always" };

  private static final String[] QualityItems = { "Choose optimal filtering automatically",
                                                 "Always use nearest neighbor filtering",
                                                 "Always use bilinear filtering" };

  private enum LabelIndex {
    ActorsAre,
    ActorsIni,
    RegionTargets,
    Entrances,
    ContainerTargets,
    Sounds,
    DoorTargets,
    Animations,
    MapNotes,
    SpawnPoints,
  }

  private final EnumMap<LabelIndex, JCheckBox> labelsMap = new EnumMap<>(LabelIndex.class);

  private SimpleListModel<LayerEntry> modelLayers;
  private JList<LayerEntry> listLayers;
  private JButton bUp;
  private JButton bDown;
  private JButton bDefaultOrder;
  private JComboBox<String> cbActorFrames;
  private JComboBox<String> cbFrames;
  private JComboBox<String> cbQualityMap;
  private JComboBox<String> cbQualityAnim;
  private JCheckBox cbShowActorSelectionCircle;
  private JCheckBox cbShowActorPersonalSpace;
  private JCheckBox cbActorAccurateBlending;
  private JCheckBox cbOverrideAnimVisibility;
  private JCheckBox cbMouseWheelZoom;
  private JCheckBox cbExportLayers;
  private JCheckBox cbUseColorShades;
  private JCheckBox cbStoreSettings;
  private JButton bDefaultSettings;
  private JButton bCancel;
  private JButton bOK;
  private JSpinner sOverlaysFps;
  private JSpinner sAnimationsFps;
  private JSlider sMiniMapAlpha;
  private boolean settingsChanged;

  public SettingsDialog(Window owner) {
    super(owner, "Area viewer settings", Dialog.ModalityType.DOCUMENT_MODAL);
    init();
  }

  public boolean settingsChanged() {
    return settingsChanged;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bUp) {
      layerUp();
    } else if (event.getSource() == bDown) {
      layerDown();
    } else if (event.getSource() == bDefaultOrder) {
      resetLayerOrder();
    } else if (event.getSource() == bDefaultSettings) {
      resetDialogSettings();
    } else if (event.getSource() == bCancel) {
      setVisible(false);
    } else if (event.getSource() == bOK) {
      updateSettings();
      setVisible(false);
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (event.getSource() == listLayers) {
      updateLayerButtons();
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // Applies dialog settings to the global Settings entries
  private void updateSettings() {
    for (int i = 0; i < modelLayers.size(); i++) {
      LayerEntry lt = modelLayers.get(i);
      Settings.LIST_LAYER_ORDER.set(i, lt.layer);
    }

    Settings.ShowLabelActorsAre =  labelsMap.get(LabelIndex.ActorsAre).isSelected();
    Settings.ShowLabelActorsIni = labelsMap.get(LabelIndex.ActorsIni).isSelected();
    Settings.ShowLabelAnimations = labelsMap.get(LabelIndex.Animations).isSelected();
    Settings.ShowLabelContainerTargets = labelsMap.get(LabelIndex.ContainerTargets).isSelected();
    Settings.ShowLabelDoorTargets = labelsMap.get(LabelIndex.DoorTargets).isSelected();
    Settings.ShowLabelEntrances = labelsMap.get(LabelIndex.Entrances).isSelected();
    Settings.ShowLabelMapNotes = labelsMap.get(LabelIndex.MapNotes).isSelected();
    Settings.ShowLabelRegionTargets = labelsMap.get(LabelIndex.RegionTargets).isSelected();
    Settings.ShowLabelSounds = labelsMap.get(LabelIndex.Sounds).isSelected();
    Settings.ShowLabelSpawnPoints = labelsMap.get(LabelIndex.SpawnPoints).isSelected();

    Settings.ShowActorFrame = cbActorFrames.getSelectedIndex();
    Settings.ShowActorSelectionCircle = cbShowActorSelectionCircle.isSelected();
    Settings.ShowActorPersonalSpace = cbShowActorPersonalSpace.isSelected();
    Settings.UseActorAccurateBlending = cbActorAccurateBlending.isSelected();

    Settings.ShowAnimationFrame = cbFrames.getSelectedIndex();
    Settings.OverrideAnimVisibility = cbOverrideAnimVisibility.isSelected();

    Settings.InterpolationMap = cbQualityMap.getSelectedIndex();
    Settings.InterpolationAnim = cbQualityAnim.getSelectedIndex();

    Settings.FrameRateOverlays = (Double) sOverlaysFps.getValue();
    Settings.FrameRateAnimations = (Double) sAnimationsFps.getValue();

    Settings.MiniMapAlpha = sMiniMapAlpha.getValue() / 100.0;

    Settings.MouseWheelZoom = cbMouseWheelZoom.isSelected();
    Settings.ExportLayers = cbExportLayers.isSelected();
    Settings.UseColorShades = cbUseColorShades.isSelected();
    Settings.StoreVisualSettings = cbStoreSettings.isSelected();

    settingsChanged = true;
  }

  // Resets layer order
  private void resetLayerOrder() {
    // filling list with layer entries
    int curIndex = listLayers.getSelectedIndex();
    modelLayers.clear();
    List<ViewerConstants.LayerStackingType> list = Settings.getDefaultLayerOrder();
    for (LayerStackingType layer : list) {
      String desc = layer.getLabel();
      modelLayers.addElement(new LayerEntry(layer, desc));
    }
    list.clear();
    list = null;
    listLayers.setSelectedIndex(curIndex);
    updateLayerButtons();
  }

  // Re-initializes dialog controls with global settings
  private void resetDialogSettings() {
    resetLayerOrder();

    labelsMap.get(LabelIndex.ActorsAre).setSelected(Settings.getDefaultLabelActorsAre());
    labelsMap.get(LabelIndex.ActorsIni).setSelected(Settings.getDefaultLabelActorsIni());
    labelsMap.get(LabelIndex.Animations).setSelected(Settings.getDefaultLabelAnimations());
    labelsMap.get(LabelIndex.ContainerTargets).setSelected(Settings.getDefaultLabelContainerTargets());
    labelsMap.get(LabelIndex.DoorTargets).setSelected(Settings.getDefaultLabelDoorTargets());
    labelsMap.get(LabelIndex.Entrances).setSelected(Settings.getDefaultLabelEntrances());
    labelsMap.get(LabelIndex.MapNotes).setSelected(Settings.getDefaultLabelMapNotes());
    labelsMap.get(LabelIndex.RegionTargets).setSelected(Settings.getDefaultLabelRegionTargets());
    labelsMap.get(LabelIndex.Sounds).setSelected(Settings.getDefaultLabelSounds());
    labelsMap.get(LabelIndex.SpawnPoints).setSelected(Settings.getDefaultLabelSpawnPoints());

    cbActorFrames.setSelectedIndex(Settings.getDefaultShowActorFrame());
    cbShowActorSelectionCircle.setSelected(Settings.getDefaultActorSelectionCircle());
    cbShowActorPersonalSpace.setSelected(Settings.getDefaultActorPersonalSpace());
    cbShowActorSelectionCircle.setSelected(Settings.getDefaultActorAccurateBlending());

    cbFrames.setSelectedIndex(Settings.getDefaultShowAnimationFrame());
    cbOverrideAnimVisibility.setSelected(Settings.getDefaultOverrideAnimVisibility());

    cbQualityMap.setSelectedIndex(Settings.getDefaultInterpolationMap());
    cbQualityAnim.setSelectedIndex(Settings.getDefaultInterpolationAnim());

    sOverlaysFps.setValue(Settings.getDefaultFrameRateOverlays());
    sAnimationsFps.setValue(Settings.getDefaultFrameRateAnimations());

    sMiniMapAlpha.setValue((int) (Settings.getDefaultMiniMapAlpha() * 100.0));

    cbUseColorShades.setSelected(Settings.getDefaultUseColorShades());
    cbStoreSettings.setSelected(Settings.getDefaultStoreVisualSettings());
  }

  private void layerUp() {
    int idx = listLayers.getSelectedIndex();
    if (idx > 0) {
      LayerEntry lt = modelLayers.get(idx);
      modelLayers.set(idx, modelLayers.get(idx - 1));
      modelLayers.set(idx - 1, lt);
      listLayers.setSelectedIndex(idx - 1);
      updateLayerButtons();
    }
  }

  private void layerDown() {
    int idx = listLayers.getSelectedIndex();
    if (idx < modelLayers.size() - 1) {
      LayerEntry lt = modelLayers.get(idx);
      modelLayers.set(idx, modelLayers.get(idx + 1));
      modelLayers.set(idx + 1, lt);
      listLayers.setSelectedIndex(idx + 1);
      updateLayerButtons();
    }
  }

  private void updateLayerButtons() {
    bUp.setEnabled(listLayers.getSelectedIndex() > 0);
    bDown.setEnabled(listLayers.getSelectedIndex() < modelLayers.size() - 1);
  }

  private void init() {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    GridBagConstraints c = new GridBagConstraints();

    settingsChanged = false;

    // Initializing layer items order
    modelLayers = new SimpleListModel<>();
    listLayers = new JList<>(modelLayers);
    listLayers.setCellRenderer(new IndexedCellRenderer(1));
    listLayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listLayers.setBorder(BorderFactory.createLineBorder(getForeground()));
    // filling list with layer entries
    for (LayerStackingType layer : Settings.LIST_LAYER_ORDER) {
      String desc = layer.getLabel();
      modelLayers.addElement(new LayerEntry(layer, desc));
    }
    Dimension d = listLayers.getPreferredSize();
    listLayers.setPreferredSize(new Dimension(d.width + 40, d.height + 4));
    listLayers.setSelectedIndex(0);
    listLayers.addListSelectionListener(this);

    bDefaultOrder = new JButton("Default stacking order");
    bDefaultOrder.addActionListener(this);

    JTextArea taOrderNote = new JTextArea();
    taOrderNote.setEditable(false);
    taOrderNote.setWrapStyleWord(true);
    taOrderNote.setLineWrap(true);
    Font fnt = UIManager.getFont("Label.font");
    Color bg = UIManager.getColor("Label.background");
    taOrderNote.setFont(fnt);
    taOrderNote.setBackground(bg);
    taOrderNote.setSelectionColor(bg);
    taOrderNote.setSelectedTextColor(bg);
    taOrderNote.setText("Note: Layers of higher priority are drawn on top of layers of lower priority.");

    JPanel pLayersArrows = new JPanel(new GridBagLayout());
    bUp = new JButton(Icons.ICON_UP_16.getIcon());
    bUp.setMargin(new Insets(16, 2, 16, 2));
    bUp.addActionListener(this);
    bUp.setEnabled(listLayers.getSelectedIndex() > 0);
    bDown = new JButton(Icons.ICON_DOWN_16.getIcon());
    bDown.setMargin(new Insets(16, 2, 16, 2));
    bDown.addActionListener(this);
    updateLayerButtons();
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    pLayersArrows.add(bUp, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    pLayersArrows.add(bDown, c);

    JPanel pLayers = new JPanel(new GridBagLayout());
    pLayers.setBorder(BorderFactory.createTitledBorder("Stacking order of layered items: "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(listLayers, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.VERTICAL,
        new Insets(4, 4, 0, 4), 0, 0);
    pLayers.add(pLayersArrows, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(bDefaultOrder, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 4, 4, 0), 0, 0);
    pLayers.add(taOrderNote, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 0), 0, 0);
    pLayers.add(new JPanel(), c);

    // Initializing options
    // Icon labels
    JPanel pShowLabels = new JPanel(new GridBagLayout());
    pShowLabels.setBorder(BorderFactory.createTitledBorder("Show icon labels for: "));
    labelsMap.put(LabelIndex.ActorsAre, new JCheckBox("Actors (ARE)"));
    labelsMap.get(LabelIndex.ActorsAre).setSelected(Settings.ShowLabelActorsAre);
    labelsMap.put(LabelIndex.ActorsIni, new JCheckBox("Actors (INI)"));
    labelsMap.get(LabelIndex.ActorsIni).setSelected(Settings.ShowLabelActorsIni);
    labelsMap.put(LabelIndex.Animations, new JCheckBox("Background Animations"));
    labelsMap.get(LabelIndex.Animations).setSelected(Settings.ShowLabelAnimations);
    labelsMap.put(LabelIndex.ContainerTargets, new JCheckBox("Container targets"));
    labelsMap.get(LabelIndex.ContainerTargets).setSelected(Settings.ShowLabelContainerTargets);
    labelsMap.put(LabelIndex.DoorTargets, new JCheckBox("Door targets"));
    labelsMap.get(LabelIndex.DoorTargets).setSelected(Settings.ShowLabelDoorTargets);
    labelsMap.put(LabelIndex.Entrances, new JCheckBox("Entrances"));
    labelsMap.get(LabelIndex.Entrances).setSelected(Settings.ShowLabelEntrances);
    labelsMap.put(LabelIndex.MapNotes, new JCheckBox("Automap Notes"));
    labelsMap.get(LabelIndex.MapNotes).setSelected(Settings.ShowLabelMapNotes);
    labelsMap.put(LabelIndex.RegionTargets, new JCheckBox("Region targets"));
    labelsMap.get(LabelIndex.RegionTargets).setSelected(Settings.ShowLabelRegionTargets);
    labelsMap.put(LabelIndex.Sounds, new JCheckBox("Ambient Sounds"));
    labelsMap.get(LabelIndex.Sounds).setSelected(Settings.ShowLabelSounds);
    labelsMap.put(LabelIndex.SpawnPoints, new JCheckBox("Spawn Points"));
    labelsMap.get(LabelIndex.SpawnPoints).setSelected(Settings.ShowLabelSpawnPoints);

    // spread entries over two columns
    for (int idx = 0, len = LabelIndex.values().length; idx < len; idx++) {
      final LabelIndex li = LabelIndex.values()[idx];
      int x = idx & 1;
      int y = idx / 2;
      int bottom = (idx == len - 1 || (idx == len - 2 && x == 0)) ? 4 : 0;
      c = ViewerUtil.setGBC(c, x, y, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 4, bottom, 4), 0, 0);
      pShowLabels.add(labelsMap.get(li), c);
    }

    // Actor animation options
    JPanel pShowActorFrame = new JPanel(new GridBagLayout());
    pShowActorFrame.setBorder(BorderFactory.createTitledBorder("Actor animations: "));
    JLabel lActorFrames = new JLabel("Show frame:");
    cbActorFrames = new JComboBox<>(AnimationFrames);
    cbActorFrames.setSelectedIndex(Settings.ShowActorFrame);
    cbShowActorSelectionCircle = new JCheckBox("Draw selection circle", Settings.ShowActorSelectionCircle);
    cbShowActorSelectionCircle
        .setToolTipText("Requires a restart of the area viewer or a map update via toolbar button.");
    cbShowActorPersonalSpace = new JCheckBox("Draw personal space indicator", Settings.ShowActorPersonalSpace);
    cbShowActorPersonalSpace
        .setToolTipText("Requires a restart of the area viewer or a map update via toolbar button.");
    cbActorAccurateBlending = new JCheckBox("Enable accurate color blending", Settings.UseActorAccurateBlending);
    cbActorAccurateBlending
        .setToolTipText("<html>Creature animations with special blending modes (such as movanic devas or wisps)<br/>"
            + "can reduce overall performance of the area viewer. Disable to improve performance.<br/>"
            + "Requires a restart of the area viewer or a map update via toolbar button.</html>");
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 0), 0, 0);
    pShowActorFrame.add(lActorFrames, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 8, 0, 4), 0, 0);
    pShowActorFrame.add(cbActorFrames, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 4), 0, 0);
    pShowActorFrame.add(cbShowActorSelectionCircle, c);
    c = ViewerUtil.setGBC(c, 0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 4), 0, 0);
    pShowActorFrame.add(cbShowActorPersonalSpace, c);
    c = ViewerUtil.setGBC(c, 0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 4), 0, 0);
    pShowActorFrame.add(cbActorAccurateBlending, c);

    // Background animation frame
    JPanel pShowFrame = new JPanel(new GridBagLayout());
    pShowFrame.setBorder(BorderFactory.createTitledBorder("Background animations: "));
    JLabel lFrames = new JLabel("Show frame:");
    cbFrames = new JComboBox<>(AnimationFrames);
    cbFrames.setSelectedIndex(Settings.ShowAnimationFrame);
    cbOverrideAnimVisibility = new JCheckBox("Show background animations regardless of their active state",
        Settings.OverrideAnimVisibility);
    cbOverrideAnimVisibility
        .setToolTipText("Requires a restart of the area viewer or a map update via toolbar button.");
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 0), 0, 0);
    pShowFrame.add(lFrames, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 8, 0, 4), 0, 0);
    pShowFrame.add(cbFrames, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 4), 0, 0);
    pShowFrame.add(cbOverrideAnimVisibility, c);

    // Interpolation type
    JPanel pQuality = new JPanel(new GridBagLayout());
    pQuality.setBorder(BorderFactory.createTitledBorder("Visual quality: "));
    JLabel lQualityMap = new JLabel("Map tilesets:");
    cbQualityMap = new JComboBox<>(QualityItems);
    cbQualityMap.setSelectedIndex(Settings.InterpolationMap);
    JLabel lQualityAnim = new JLabel("Animations:");
    cbQualityAnim = new JComboBox<>(QualityItems);
    cbQualityAnim.setSelectedIndex(Settings.InterpolationAnim);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 0), 0, 0);
    pQuality.add(lQualityMap, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 4), 0, 0);
    pQuality.add(cbQualityMap, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 0), 0, 0);
    pQuality.add(lQualityAnim, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 4, 4), 0, 0);
    pQuality.add(cbQualityAnim, c);

    // Frame rates
    JPanel pFrameRates = new JPanel(new GridBagLayout());
    pFrameRates.setBorder(BorderFactory.createTitledBorder("Frame rates: "));
    JTextArea taFrameRatesNote = new JTextArea();
    taFrameRatesNote.setEditable(false);
    taFrameRatesNote.setWrapStyleWord(true);
    taFrameRatesNote.setLineWrap(true);
    taFrameRatesNote.setFont(fnt);
    taFrameRatesNote.setBackground(bg);
    taFrameRatesNote.setSelectionColor(bg);
    taFrameRatesNote.setSelectedTextColor(bg);
    taFrameRatesNote.setText("Caution: The area viewer may become less responsive on higher frame rates.");

    sOverlaysFps = new JSpinner(new SpinnerNumberModel(Settings.FrameRateOverlays, 1.0, 30.0, 0.5));
    d = sOverlaysFps.getPreferredSize();
    sOverlaysFps.setPreferredSize(new Dimension(d.width + 16, d.height));
    sAnimationsFps = new JSpinner(new SpinnerNumberModel(Settings.FrameRateAnimations, 1.0, 30.0, 0.5));
    d = sAnimationsFps.getPreferredSize();
    sAnimationsFps.setPreferredSize(new Dimension(d.width + 16, d.height));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("Overlays:"), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 4, 0), 0, 0);
    pFrameRates.add(sOverlaysFps, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("fps"), c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 16, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("Animations:"), c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 4, 0), 0, 0);
    pFrameRates.add(sAnimationsFps, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 4, 4), 0, 0);
    pFrameRates.add(new JLabel("fps"), c);
    c = ViewerUtil.setGBC(c, 0, 1, 6, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 4, 4), 0, 0);
    pFrameRates.add(taFrameRatesNote, c);

    // Minimap transparency
    JPanel pMiniMap = new JPanel(new GridBagLayout());
    pMiniMap.setBorder(BorderFactory.createTitledBorder("Mini map opacity: "));

    Hashtable<Integer, JLabel> table = new Hashtable<>();
    for (int i = 0; i <= 100; i += 25) {
      table.put(i, new JLabel(String.format("%d%%", i)));
    }
    sMiniMapAlpha = new JSlider(0, 100, (int) (Settings.MiniMapAlpha * 100.0));
    sMiniMapAlpha.setSnapToTicks(false);
    sMiniMapAlpha.setLabelTable(table);
    sMiniMapAlpha.setPaintLabels(true);
    sMiniMapAlpha.setMinorTickSpacing(5);
    sMiniMapAlpha.setMajorTickSpacing(25);
    sMiniMapAlpha.setPaintTicks(true);
    sMiniMapAlpha.setPaintTrack(true);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 4, 4, 4), 0, 0);
    pMiniMap.add(sMiniMapAlpha, c);

    // Misc. settings
    JPanel pMisc = new JPanel(new GridBagLayout());
    pMisc.setBorder(BorderFactory.createTitledBorder("Misc. settings: "));
    cbExportLayers = new JCheckBox("Include layer items when exporting map as graphics");
    cbExportLayers.setSelected(Settings.ExportLayers);
    cbUseColorShades = new JCheckBox("Use individual color shades for region types");
    cbUseColorShades.setSelected(Settings.UseColorShades);
    cbMouseWheelZoom = new JCheckBox("Use mouse wheel to zoom map");
    cbMouseWheelZoom.setSelected(Settings.MouseWheelZoom);
    cbStoreSettings = new JCheckBox("Remember all visual settings");
    cbStoreSettings.setSelected(Settings.StoreVisualSettings);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 4), 0, 0);
    pMisc.add(cbExportLayers, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 4), 0, 0);
    pMisc.add(cbUseColorShades, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 4), 0, 0);
    pMisc.add(cbMouseWheelZoom, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 4, 4), 0, 0);
    pMisc.add(cbStoreSettings, c);

    // initializing dialog buttons
    JPanel pButtons = new JPanel(new GridBagLayout());
    bDefaultSettings = new JButton("Set defaults");
    bDefaultSettings.addActionListener(this);
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    bOK = new JButton("OK");
    bOK.setPreferredSize(bCancel.getPreferredSize());
    bOK.addActionListener(this);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bDefaultSettings, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bOK, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    JPanel pCol1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    pCol1.add(pLayers, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol1.add(pShowLabels, c);

    // putting options together
    JPanel pCol2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    pCol2.add(pShowActorFrame, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol2.add(pShowFrame, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol2.add(pQuality, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol2.add(pFrameRates, c);
    c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol2.add(pMiniMap, c);
    c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    pCol2.add(pMisc, c);
    c = ViewerUtil.setGBC(c, 0, 6, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    pCol2.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 0, 7, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    pCol2.add(pButtons, c);

    // putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 0), 0, 0);
    pMain.add(pCol1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 8), 0, 0);
    pMain.add(pCol2, c);

    getContentPane().add(pMain, BorderLayout.CENTER);
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getOwner());

    // Registering close on ESC key
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        getRootPane());
    getRootPane().getActionMap().put(getRootPane(), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        setVisible(false);
      }
    });

    getRootPane().setDefaultButton(bOK);
    setVisible(true);
  }

  // ----------------------------- INNER CLASSES -----------------------------

  private static class IndexedCellRenderer extends DefaultListCellRenderer {
    private final int startIndex;

    public IndexedCellRenderer(int startIndex) {
      super();
      this.startIndex = startIndex;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      String template = "%0" + String.format("%d", Integer.toString(list.getModel().getSize()).length()) + "d - %s";
      return super.getListCellRendererComponent(list, String.format(template, index + startIndex, value), index,
          isSelected, cellHasFocus);
    }
  }

  // A simple wrapper for the list control to link layers with their respective description
  private static class LayerEntry {
    public ViewerConstants.LayerStackingType layer;
    public String desc;

    public LayerEntry(ViewerConstants.LayerStackingType layer, String description) {
      this.layer = layer;
      this.desc = description;
    }

    @Override
    public String toString() {
      return desc;
    }
  }
}
