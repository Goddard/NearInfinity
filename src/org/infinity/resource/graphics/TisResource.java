// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.RootPaneContainer;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.TileGrid;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.converter.ConvertToPvrz;
import org.infinity.gui.converter.ConvertToTis;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceSearcher;
import org.infinity.util.BinPack2D;
import org.infinity.util.DynamicArray;
import org.infinity.util.IntegerHashMap;
import org.infinity.util.io.StreamUtils;

public class TisResource implements Resource, Closeable, ActionListener, ChangeListener,
                                     ItemListener, KeyListener, PropertyChangeListener
{
  private enum Status { SUCCESS, CANCELLED, ERROR, UNSUPPORTED }

  private static final Color TransparentColor = new Color(0, true);
  private static final int DEFAULT_COLUMNS = 5;

  private static boolean showGrid = false;

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private TisDecoder decoder;
  private List<Image> tileImages;         // stores one tile per image
  private TileGrid tileGrid;              // the main component for displaying the tileset
  private JSlider slCols;                 // changes the tiles per row
  private JTextField tfCols;              // input/output tiles per row
  private JCheckBox cbGrid;               // show/hide frame around each tile
  private JMenuItem miExport, miExportPaletteTis, miExportPvrzTis, miExportPNG;
  private JPanel panel;                   // top-level panel of the viewer
  private RootPaneContainer rpc;
  private SwingWorker<Status, Void> workerToPalettedTis, workerToPvrzTis, workerExport;
  private WindowBlocker blocker;
  private int defaultWidth;

  public TisResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    initTileset();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES)) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExportPaletteTis) {
      final Path tisFile = getTisFileName(panel.getTopLevelAncestor(), false);
      if (tisFile != null) {
        blocker = new WindowBlocker(rpc);
        blocker.setBlocked(true);
        workerToPalettedTis = new SwingWorker<Status, Void>() {
          @Override
          public Status doInBackground()
          {
            Status retVal = Status.ERROR;
            try {
              retVal = convertToPaletteTis(tisFile, true);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return retVal;
          }
        };
        workerToPalettedTis.addPropertyChangeListener(this);
        workerToPalettedTis.execute();
      }
    } else if (event.getSource() == miExportPvrzTis) {
      final Path tisFile = getTisFileName(panel.getTopLevelAncestor(), true);
      if (tisFile != null) {
        blocker = new WindowBlocker(rpc);
        blocker.setBlocked(true);
        workerToPvrzTis = new SwingWorker<Status, Void>() {
          @Override
          public Status doInBackground()
          {
            Status retVal = Status.ERROR;
            try {
              retVal = convertToPvrzTis(tisFile, true);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return retVal;
          }
        };
        workerToPvrzTis.addPropertyChangeListener(this);
        workerToPvrzTis.execute();
      }
    } else if (event.getSource() == miExportPNG) {
      final Path pngFile = getPngFileName(panel.getTopLevelAncestor());
      if (pngFile != null) {
        blocker = new WindowBlocker(rpc);
        blocker.setBlocked(true);
        workerExport = new SwingWorker<Status, Void>() {
          @Override
          public Status doInBackground()
          {
            Status retVal = Status.ERROR;
            try {
              retVal = exportPNG(pngFile, true);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return retVal;
          }
        };
        workerExport.addPropertyChangeListener(this);
        workerExport.execute();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------


//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slCols) {
      int cols = slCols.getValue();
      tfCols.setText(Integer.toString(cols));
      tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbGrid) {
      showGrid = cbGrid.isSelected();
      tileGrid.setShowGrid(showGrid);
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface KeyListener ---------------------

  @Override
  public void keyPressed(KeyEvent event)
  {
    if (event.getSource() == tfCols) {
      if (event.getKeyCode() == KeyEvent.VK_ENTER) {
        int cols;
        try {
          cols = Integer.parseInt(tfCols.getText());
        } catch (NumberFormatException e) {
          cols = slCols.getValue();
          tfCols.setText(Integer.toString(slCols.getValue()));
        }
        if (cols != slCols.getValue()) {
          if (cols <= 0)
            cols = 1;
          if (cols >= decoder.getTileCount())
            cols = decoder.getTileCount();
          slCols.setValue(cols);
          tfCols.setText(Integer.toString(slCols.getValue()));
          tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
        }
        slCols.requestFocus();    // remove focus from textfield
      }
    }
  }


  @Override
  public void keyReleased(KeyEvent event)
  {
    // nothing to do
  }


  @Override
  public void keyTyped(KeyEvent event)
  {
    // nothing to do
  }

//--------------------- End Interface KeyListener ---------------------

//--------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent event)
  {
    if (event.getSource() instanceof SwingWorker<?, ?>) {
      @SuppressWarnings("unchecked")
      SwingWorker<Status, Void> worker = (SwingWorker<Status, Void>)event.getSource();
      if ("state".equals(event.getPropertyName()) &&
          SwingWorker.StateValue.DONE == event.getNewValue()) {
        if (blocker != null) {
          blocker.setBlocked(false);
          blocker = null;
        }
        Status retVal = Status.ERROR;
        try {
          retVal = worker.get();
          if (retVal == null) {
            retVal = Status.ERROR;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (retVal == Status.SUCCESS) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "File exported successfully.", "Export complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } else if (retVal == Status.CANCELLED) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Export has been cancelled.", "Information",
                                        JOptionPane.INFORMATION_MESSAGE);
        } else if (retVal == Status.UNSUPPORTED) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Operation not (yet) supported.", "Information",
                                        JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (workerToPalettedTis != null) {
      if (!workerToPalettedTis.isDone()) {
        workerToPalettedTis.cancel(true);
      }
      workerToPalettedTis = null;
    }
    if (workerToPvrzTis != null) {
      if (!workerToPvrzTis.isDone()) {
        workerToPvrzTis.cancel(true);
      }
      workerToPvrzTis = null;
    }
    if (workerExport != null) {
      if (!workerExport.isDone()) {
        workerExport.cancel(true);
      }
      workerExport = null;
    }
    if (tileImages != null) {
      tileImages.clear();
      tileImages = null;
    }
    if (tileGrid != null) {
      tileGrid.clearImages();
      tileGrid = null;
    }
    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
    System.gc();
  }

//--------------------- End Interface Closeable ---------------------


//--------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------


//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    if (container instanceof RootPaneContainer) {
      rpc = (RootPaneContainer)container;
    } else {
      rpc = NearInfinity.getInstance();
    }

    if (decoder == null) {
      return new JPanel(new BorderLayout());
    }

    int tileCount = decoder.getTileCount();
    int defaultColumns = Math.min(tileCount, DEFAULT_COLUMNS);

    // 1. creating top panel
    // 1.1. creating label with text field
    JLabel lblTPR = new JLabel("Tiles per row:");
    tfCols = new JTextField(Integer.toString(defaultColumns), 5);
    tfCols.addKeyListener(this);
    JPanel tPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
    tPanel1.add(lblTPR);
    tPanel1.add(tfCols);

    // 1.2. creating slider
    slCols = new JSlider(JSlider.HORIZONTAL, 1, tileCount, defaultColumns);
    if (tileCount > 1000) {
      slCols.setMinorTickSpacing(100);
      slCols.setMajorTickSpacing(1000);
    } else if (tileCount > 100) {
      slCols.setMinorTickSpacing(10);
      slCols.setMajorTickSpacing(100);
    } else {
      slCols.setMinorTickSpacing(1);
      slCols.setMajorTickSpacing(10);
    }
    slCols.setPaintTicks(true);
    slCols.addChangeListener(this);

    // 1.3. adding left side of the top panel together
    JPanel tlPanel = new JPanel(new GridLayout(2, 1));
    tlPanel.add(tPanel1);
    tlPanel.add(slCols);

    // 1.4. configuring checkbox
    cbGrid = new JCheckBox("Show Grid", showGrid);
    cbGrid.addItemListener(this);
    JPanel trPanel = new JPanel(new GridLayout());
    trPanel.add(cbGrid);

    // 1.5. putting top panel together
    BorderLayout bl = new BorderLayout();
    JPanel topPanel = new JPanel(bl);
    topPanel.add(tlPanel, BorderLayout.CENTER);
    topPanel.add(trPanel, BorderLayout.LINE_END);

    // 2. creating main panel
    // 2.1. creating tiles table and scroll pane
    tileGrid = new TileGrid(1, defaultColumns, decoder.getTileWidth(), decoder.getTileHeight());
    tileGrid.addImage(tileImages);
    tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), getDefaultTilesPerRow()));
    tileGrid.setShowGrid(showGrid);
    slCols.setValue(tileGrid.getTileColumns());
    tfCols.setText(Integer.toString(tileGrid.getTileColumns()));
    JScrollPane scroll = new JScrollPane(tileGrid);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    // 2.2. putting main panel together
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(scroll, BorderLayout.CENTER);

    // 3. creating bottom panel
    // 3.1. creating export button
    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    if (decoder.getType() == TisDecoder.Type.PVRZ) {
      miExportPaletteTis = new JMenuItem("as palette-based TIS");
      miExportPaletteTis.addActionListener(this);
    } else if (decoder.getType() == TisDecoder.Type.PALETTE) {
      miExportPvrzTis = new JMenuItem("as PVRZ-based TIS");
      miExportPvrzTis.addActionListener(this);
    }
    miExportPNG = new JMenuItem("as PNG");
    miExportPNG.addActionListener(this);

    List<JMenuItem> list = new ArrayList<JMenuItem>();
    if (miExport != null)
      list.add(miExport);
    if (miExportPaletteTis != null) {
      list.add(miExportPaletteTis);
    }
    if (miExportPvrzTis != null) {
      list.add(miExportPvrzTis);
    }
    if (miExportPNG != null)
      list.add(miExportPNG);
    JMenuItem[] mi = new JMenuItem[list.size()];
    for (int i = 0; i < mi.length; i++) {
      mi[i] = list.get(i);
    }
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    ButtonPopupMenu bpmExport = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.EXPORT_MENU);
    bpmExport.setMenuItems(mi);

    // 4. packing all together
    panel = new JPanel(new BorderLayout());
    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  // Returns detected or guessed number of tiles per row of the current TIS
  private int getDefaultTilesPerRow()
  {
    return defaultWidth;
  }

  // Returns an output filename for a TIS file
  private Path getTisFileName(Component parent, boolean enforceValidName)
  {
    Path retVal = null;
    JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    fc.setDialogTitle("Export resource");
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("TIS files (*.tis)", "tis");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), getResourceEntry().getResourceName()));
    boolean repeat = enforceValidName;
    do {
      retVal = null;
      if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
        retVal = fc.getSelectedFile().toPath();
        if (enforceValidName && !isTisFileNameValid(retVal)) {
          JOptionPane.showMessageDialog(parent,
                                        "PVRZ-based TIS filenames have to be 2 up to 7 characters long.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        } else {
          repeat = false;
        }
        if (Files.exists(retVal)) {
          final String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(parent, retVal + " exists. Overwrite?", "Export resource",
                                           JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                           null, options, options[0]) != 0) {
            retVal = null;
            repeat = false;
          }
        }
      } else {
        repeat = false;
      }
    } while (repeat);
    return retVal;
  }

  // Returns output filename for a PNG file
  private Path getPngFileName(Component parent)
  {
    Path retVal = null;
    JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    fc.setDialogTitle("Export resource");
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files (*.png)", "png");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH).replace(".TIS", ".PNG")));
    if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
      retVal = fc.getSelectedFile().toPath();
      if (Files.exists(retVal)) {
        final String options[] = {"Overwrite", "Cancel"};
        if (JOptionPane.showOptionDialog(parent, retVal + " exists. Overwrite?", "Export resource",
                                         JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                         null, options, options[0]) != 0) {
          retVal = null;
        }
      }
    }
    return retVal;
  }

  private void initTileset()
  {
    try {
      WindowBlocker.blockWindow(true);

      decoder = TisDecoder.loadTis(entry);
      if (decoder != null) {
        int tileCount = decoder.getTileCount();
        defaultWidth = calcTileWidth(entry, tileCount);
        tileImages = new ArrayList<Image>(tileCount);
        for (int tileIdx = 0; tileIdx < tileCount; tileIdx++) {
          BufferedImage image = ColorConvert.createCompatibleImage(64, 64, Transparency.BITMASK);
          decoder.getTile(tileIdx, image);
          tileImages.add(image);
        }
      } else {
        throw new Exception("No TIS resource loaded");
      }
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      e.printStackTrace();
      WindowBlocker.blockWindow(false);
      if (tileImages == null)
        tileImages = new ArrayList<Image>();
      if (tileImages.isEmpty())
        tileImages.add(ColorConvert.createCompatibleImage(1, 1, Transparency.BITMASK));
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                    "Error while loading TIS resource: " + entry.getResourceName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Converts the current PVRZ-based tileset into the old tileset variant.
  public Status convertToPaletteTis(Path output, boolean showProgress)
  {
    Status retVal = Status.ERROR;
    if (output != null) {
      if (tileImages != null && !tileImages.isEmpty()) {
        String note = "Converting tile %1$d / %2$d";
        int progressIndex = 0, progressMax = decoder.getTileCount();
        ProgressMonitor progress = null;
        if (showProgress) {
          progress = new ProgressMonitor(panel.getTopLevelAncestor(), "Converting TIS...",
                                         String.format(note, progressIndex, progressMax), 0, progressMax);
          progress.setMillisToDecideToPopup(500);
          progress.setMillisToPopup(2000);
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(output))) {
          retVal = Status.SUCCESS;

          // writing header data
          byte[] header = new byte[24];
          System.arraycopy("TIS V1  ".getBytes(), 0, header, 0, 8);
          DynamicArray.putInt(header, 8, decoder.getTileCount());
          DynamicArray.putInt(header, 12, 0x1400);
          DynamicArray.putInt(header, 16, 0x18);
          DynamicArray.putInt(header, 20, 0x40);
          bos.write(header);

          // writing tile data
          int[] palette = new int[255];
          int[] hclPalette = new int[255];
          byte[] tilePalette = new byte[1024];
          byte[] tileData = new byte[64*64];
          BufferedImage image =
              ColorConvert.createCompatibleImage(decoder.getTileWidth(), decoder.getTileHeight(),
                                                 Transparency.BITMASK);
          IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(1800);   // caching RGBColor -> index
          for (int tileIdx = 0; tileIdx < decoder.getTileCount(); tileIdx++) {
            colorCache.clear();
            if (progress != null && progress.isCanceled()) {
              retVal = Status.CANCELLED;
              break;
            }
            progressIndex++;
            if (progress != null && (progressIndex % 100) == 0) {
              progress.setProgress(progressIndex);
              progress.setNote(String.format(note, progressIndex, progressMax));
            }

            Graphics2D g = image.createGraphics();
            try {
              g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
              g.setColor(TransparentColor);
              g.fillRect(0, 0, image.getWidth(), image.getHeight());
              g.drawImage(tileImages.get(tileIdx), 0, 0, null);
            } finally {
              g.dispose();
              g = null;
            }

            int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            if (ColorConvert.medianCut(pixels, 255, palette, false)) {
              ColorConvert.toHclPalette(palette, hclPalette);
              // filling palette
              // first palette entry denotes transparency
              tilePalette[0] = tilePalette[2] = tilePalette[3] = 0; tilePalette[1] = (byte)255;
              for (int i = 1; i < 256; i++) {
                tilePalette[(i << 2) + 0] = (byte)(palette[i - 1] & 0xff);
                tilePalette[(i << 2) + 1] = (byte)((palette[i - 1] >>> 8) & 0xff);
                tilePalette[(i << 2) + 2] = (byte)((palette[i - 1] >>> 16) & 0xff);
                tilePalette[(i << 2) + 3] = 0;
                colorCache.put(palette[i - 1], (byte)(i - 1));
              }
              // filling pixel data
              for (int i = 0; i < tileData.length; i++) {
                if ((pixels[i] & 0xff000000) == 0) {
                  tileData[i] = 0;
                } else {
                  Byte palIndex = colorCache.get(pixels[i]);
                  if (palIndex != null) {
                    tileData[i] = (byte)(palIndex + 1);
                  } else {
                    byte color = (byte)ColorConvert.nearestColor(pixels[i], hclPalette);
                    tileData[i] = (byte)(color + 1);
                    colorCache.put(pixels[i], color);
                  }
                }
              }
            } else {
              retVal = Status.ERROR;
              break;
            }
            bos.write(tilePalette);
            bos.write(tileData);
          }
          image.flush(); image = null;
          tileData = null; tilePalette = null; hclPalette = null; palette = null;
        } catch (Exception e) {
          retVal = Status.ERROR;
          e.printStackTrace();
        } finally {
          if (progress != null) {
            progress.close();
            progress = null;
          }
        }
        if (retVal != Status.SUCCESS && Files.isRegularFile(output)) {
          try {
            Files.delete(output);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return retVal;
  }

  // Converts the current palette-based tileset into the new PVRZ-based variant.
  public Status convertToPvrzTis(Path output, boolean showProgress)
  {
    Status retVal = Status.ERROR;
    if (output != null) {
      try {
        ProgressMonitor progress = null;
        if (showProgress) {
          progress = new ProgressMonitor(panel.getTopLevelAncestor(),
                                         "Converting TIS...", "Preparing TIS", 0, 5);
          progress.setMillisToDecideToPopup(0);
          progress.setMillisToPopup(0);
        }

        int numTiles = decoder.getTileCount();
        int tilesPerRow = getDefaultTilesPerRow();
        List<ConvertToTis.TileEntry> entryList = new ArrayList<ConvertToTis.TileEntry>(numTiles);
        List<BinPack2D> pageList = new ArrayList<BinPack2D>();

        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(output))) {
          // writing header data
          byte[] header = new byte[24];
          System.arraycopy("TIS V1  ".getBytes(), 0, header, 0, 8);
          DynamicArray.putInt(header, 8, numTiles);
          DynamicArray.putInt(header, 12, 0x0c);
          DynamicArray.putInt(header, 16, 0x18);
          DynamicArray.putInt(header, 20, 0x40);
          bos.write(header);

          // processing tiles
          final BinPack2D.HeuristicRules binPackRule = BinPack2D.HeuristicRules.BOTTOM_LEFT_RULE;
          final int pageDim = 1024;
          final int tileDim = 64;
          final int tilesPerDim = pageDim / tileDim;
          int tisWidth = tilesPerRow*tileDim;
          int tisHeight = ((numTiles+tilesPerRow-1) / tilesPerRow) * tileDim;
          int pw = tisWidth / pageDim + (((tisWidth % pageDim) != 0) ? 1 : 0);
          int ph = tisHeight / pageDim + (((tisHeight % pageDim) != 0) ? 1 : 0);

          for (int py = 0; py < ph; py++) {
            for (int px = 0; px < pw; px++) {
              int x = px * pageDim, y = py * pageDim;
              int w = Math.min(pageDim, tisWidth - x);
              int h = Math.min(pageDim, tisHeight - y);

              Dimension space = new Dimension(w / tileDim, h / tileDim);
              int pageIdx = -1;
              Rectangle rectMatch = null;
              for (int i = 0; i < pageList.size(); i++) {
                BinPack2D packer = pageList.get(i);
                rectMatch = packer.insert(space.width, space.height, binPackRule);
                if (rectMatch.height > 0) {
                  pageIdx = i;
                  break;
                }
              }

              // create new page?
              if (pageIdx < 0) {
                BinPack2D packer = new BinPack2D(tilesPerDim, tilesPerDim);
                pageList.add(packer);
                pageIdx = pageList.size() - 1;
                rectMatch = packer.insert(space.width, space.height, binPackRule);
              }

              // registering tile entries
              int tileIdx = (y*tisWidth)/(tileDim*tileDim) + x/tileDim;
              for (int ty = 0; ty < space.height; ty++, tileIdx += tisWidth/tileDim) {
                for (int tx = 0; tx < space.width; tx++) {
                  // marking page index as incomplete
                  if (tileIdx + tx < numTiles) {
                    ConvertToTis.TileEntry entry =
                        new ConvertToTis.TileEntry(tileIdx + tx, pageIdx,
                                                   (rectMatch.x + tx)*tileDim,
                                                   (rectMatch.y + ty)*tileDim);
                    entryList.add(entry);
                  }
                }
              }
            }
          }

          // writing TIS entries
          Collections.sort(entryList, ConvertToTis.TileEntry.CompareByIndex);
          for (int i = 0; i < entryList.size(); i++) {
            ConvertToTis.TileEntry entry = entryList.get(i);
            bos.write(DynamicArray.convertInt(entry.page));
            bos.write(DynamicArray.convertInt(entry.x));
            bos.write(DynamicArray.convertInt(entry.y));
          }

          // generating PVRZ files
          retVal = writePvrzPages(output, pageList, entryList, progress);
        } finally {
          if (progress != null) {
            progress.close();
            progress = null;
          }
        }
      } catch (Exception e) {
        retVal = Status.ERROR;
        e.printStackTrace();
      }
      if (retVal != Status.SUCCESS && Files.isRegularFile(output)) {
        try {
          Files.delete(output);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  // Converts the tileset into the PNG format.
  public Status exportPNG(Path output, boolean showProgress)
  {
    Status retVal = Status.ERROR;
    if (output != null) {
      if (tileImages != null && !tileImages.isEmpty()) {
        int tilesX = tileGrid.getTileColumns();
        int tilesY = tileGrid.getTileRows();
        if (tilesX > 0 && tilesY > 0) {
          BufferedImage image = null;
          ProgressMonitor progress = null;
          if (showProgress) {
            progress = new ProgressMonitor(panel.getTopLevelAncestor(), "Exporting TIS to PNG...", "", 0, 2);
            progress.setMillisToDecideToPopup(0);
            progress.setMillisToPopup(0);
            progress.setProgress(0);
          }
            image = ColorConvert.createCompatibleImage(tilesX*64, tilesY*64, Transparency.BITMASK);
            Graphics2D g = image.createGraphics();
            for (int idx = 0; idx < tileImages.size(); idx++) {
              if (tileImages.get(idx) != null) {
                int tx = idx % tilesX;
                int ty = idx / tilesX;
                g.drawImage(tileImages.get(idx), tx*64, ty*64, null);
              }
            }
            g.dispose();

            if (progress != null) {
              progress.setProgress(1);
            }
            try (OutputStream os = StreamUtils.getOutputStream(output, true)) {
              if (ImageIO.write(image, "png", os)) {
                retVal = Status.SUCCESS;
              }
            } catch (IOException e) {
              retVal = Status.ERROR;
              e.printStackTrace();
            }
          if (progress != null && progress.isCanceled()) {
            retVal = Status.CANCELLED;
          }
          if (progress != null) {
            progress.close();
            progress = null;
          }
        }
        if (retVal != Status.SUCCESS && Files.isRegularFile(output)) {
          try {
            Files.delete(output);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return retVal;
  }

  // Generates PVRZ files based on the current TIS resource and the specified parameters
  private Status writePvrzPages(Path tisFile, List<BinPack2D> pageList,
                                 List<ConvertToTis.TileEntry> entryList, ProgressMonitor progress)
  {
    Status retVal = Status.SUCCESS;
    DxtEncoder.DxtType dxtType = DxtEncoder.DxtType.DXT1;
    int dxtCode = 7;  // PVR code for DXT1
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    String note = "Generating PVRZ file %1$s / %2$s";
    if (progress != null) {
      progress.setMaximum(pageList.size() + 1);
      progress.setProgress(1);
    }

    try {
      for (int pageIdx = 0; pageIdx < pageList.size(); pageIdx++) {
        if (progress != null) {
          if (progress.isCanceled()) {
            retVal = Status.CANCELLED;
            return retVal;
          }
          progress.setProgress(pageIdx + 1);
          progress.setNote(String.format(note, pageIdx+1, pageList.size()));
        }

        Path pvrzFile = generatePvrzFileName(tisFile, pageIdx);
        BinPack2D packer = pageList.get(pageIdx);
        packer.shrinkBin(true);

        // generating texture image
        int w = packer.getBinWidth() * 64;
        int h = packer.getBinHeight() * 64;
        BufferedImage texture = ColorConvert.createCompatibleImage(w, h, true);
        Graphics2D g = texture.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        try {
          g.setBackground(new Color(0, true));
          g.setColor(Color.BLACK);
          g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
          for (final ConvertToTis.TileEntry entry: entryList) {
            if (entry.page == pageIdx) {
              Image tileImg = decoder.getTile(entry.tileIndex);
              int dx = entry.x, dy = entry.y;
              g.drawImage(tileImg, dx, dy, dx+64, dy+64, 0, 0, 64, 64, null);
            }
          }
        } finally {
          g.dispose();
          g = null;
        }

        int[] textureData = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
        try {
          // compressing PVRZ
          int outSize = DxtEncoder.calcImageSize(texture.getWidth(), texture.getHeight(), dxtType);
          DxtEncoder.encodeImage(textureData, texture.getWidth(), texture.getHeight(), output, dxtType);
          byte[] header = ConvertToPvrz.createPVRHeader(texture.getWidth(), texture.getHeight(), dxtCode);
          byte[] pvrz = new byte[header.length + outSize];
          System.arraycopy(header, 0, pvrz, 0, header.length);
          System.arraycopy(output, 0, pvrz, header.length, outSize);
          header = null;
          pvrz = Compressor.compress(pvrz, 0, pvrz.length, true);

          // writing PVRZ to disk
          try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(pvrzFile))) {
            bos.write(pvrz);
          } catch (IOException e) {
            retVal = Status.ERROR;
            e.printStackTrace();
            return retVal;
          }
          pvrz = null;
        } catch (Exception e) {
          retVal = Status.ERROR;
          e.printStackTrace();
          return retVal;
        }
      }
    } finally {
      // cleaning up
      if (retVal != Status.SUCCESS) {
        for (int i = 0; i < pageList.size(); i++) {
          Path pvrzFile = generatePvrzFileName(tisFile, i);
          if (pvrzFile != null && Files.isRegularFile(pvrzFile)) {
            try {
              Files.delete(pvrzFile);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
    return retVal;
  }

  // Generates PVRZ filename with full path from the given parameters
  private Path generatePvrzFileName(Path tisFile, int page)
  {
    if (tisFile != null) {
      Path path = tisFile.getParent();
      String tisName = tisFile.getFileName().toString();
      int extOfs = tisName.lastIndexOf('.');
      if (extOfs > 0) {
        tisName = tisName.substring(0, extOfs);
      }
      if (Pattern.matches(".{2,7}", tisName)) {
        String pvrzName = String.format("%1$s%2$s%3$02d.PVRZ", tisName.substring(0, 1),
                                        tisName.substring(2, tisName.length()), page);
        return path.resolve(pvrzName);
      }
    }
    return null;
  }

  // Returns true only if TIS filename can be used to generate PVRZ filenames from
  public static boolean isTisFileNameValid(Path fileName)
  {
    if (fileName != null) {
      String name = fileName.getFileName().toString();
      int extOfs = name.lastIndexOf('.');
      if (extOfs >= 0) {
        name = name.substring(0, extOfs);
      }
      return Pattern.matches(".{2,7}", name);
    }
    return false;
  }

  // Attempts to fix the specified filename to make it compatible with the naming scheme of TIS V2 files
  public static Path makeTisFileNameValid(Path fileName)
  {
    if (fileName != null && !isTisFileNameValid(fileName)) {
      Path path = fileName.getParent();
      String name = fileName.getFileName().toString();
      String ext = "";
      int extOfs = name.lastIndexOf('.');
      if (extOfs >= 0) {
        ext = name.substring(extOfs);
        name = name.substring(0, extOfs);
      }

      boolean isNight = (Character.toUpperCase(name.charAt(name.length() - 1)) == 'N');
      if (name.length() > 7) {
        int numDelete = name.length() - 7;
        int ofsDelete = name.length() - numDelete - (isNight ? 1 : 0);
        name = name.substring(ofsDelete, numDelete);
        return path.resolve(name);
      } else if (name.length() < 2) {
        String fmt, newName = null;
        int maxNum;
        switch (name.length()) {
          case 0:  fmt = name + "%1$s02d"; maxNum = 99; break;
          default: fmt = name + "%1$s01d"; maxNum = 9; break;
        }
        for (int i = 0; i < maxNum; i++) {
          String s = String.format(fmt, i) + (isNight ? "N" : "") + ext;
          if (!ResourceFactory.resourceExists(s)) {
            newName = s;
            break;
          }
        }
        if (newName != null) {
          return path.resolve(newName);
        }
      }
    }
    return fileName;
  }

  /**
   * Attempts to calculate the TIS width from an associated WED file.
   * @param entry The TIS resource entry.
   * @param tileCount An optional tile count that will be used to "guess" the correct number of tiles
   *                  per row if no associated WED resource has been found.
   * @return The number of tiles per row for the specified TIS resource.
   */
  public static int calcTileWidth(ResourceEntry entry, int tileCount)
  {
    // Try to fetch the correct width from an associated WED if available
    if (entry != null) {
      try {
        String tisNameBase = entry.getResourceName();
        if (tisNameBase.lastIndexOf('.') > 0) {
          tisNameBase = tisNameBase.substring(0, tisNameBase.lastIndexOf('.'));
        }
        ResourceEntry wedEntry = null;
        while (tisNameBase.length() >= 6) {
          String wedFileName = tisNameBase + ".WED";
          wedEntry = ResourceFactory.getResourceEntry(wedFileName);
          if (wedEntry != null) {
            break;
          } else {
            tisNameBase = tisNameBase.substring(0, tisNameBase.length() - 1);
          }
        }
        if (wedEntry != null) {
          ByteBuffer wed = wedEntry.getResourceBuffer();
          if (wed != null) {
            String sig = StreamUtils.readString(wed, 0, 8);
            if (sig.equals("WED V1.3")) {
              final int sizeOvl = 0x18;
              int numOvl = wed.getInt(8);
              int ofsOvl = wed.getInt(16);
              for (int i = 0; i < numOvl; i++) {
                int ofs = ofsOvl + i*sizeOvl;
                String tisName = StreamUtils.readString(wed, ofs + 4, 8);
                if (tisName.equalsIgnoreCase(tisNameBase)) {
                  int width = wed.getShort(ofs);
                  if (width > 0) {
                    return width;
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
      }
    }
    // If WED is not available: approximate the most commonly used aspect ratio found in TIS files
    // Disadvantage: does not take extra tiles into account
    return (tileCount < 9) ? tileCount : (int)(Math.sqrt(tileCount)*1.18);
  }

  // Calculates a Dimension structure with the correct number of columns and rows from the specified arguments
  private static Dimension calcGridSize(int imageCount, int colSize)
  {
    if (imageCount >= 0 && colSize > 0) {
      int rowSize = imageCount / colSize;
      if (imageCount % colSize > 0)
        rowSize++;
      return new Dimension(colSize, Math.max(1, rowSize));
    }
    return null;
  }

  /** Returns whether the specified PVRZ index can be found in the current TIS resource. */
  public boolean containsPvrzReference(int index)
  {
    boolean retVal = false;
    if (index >= 0 && index <= 99) {
      if (decoder instanceof TisV2Decoder) {
        TisV2Decoder tisv2 = (TisV2Decoder)decoder;
        for (int i = 0, count = tisv2.getTileCount(); i < count && !retVal; i++) {
          retVal = (tisv2.getPvrzPage(i) == index);
        }
      }
    }
    return retVal;
  }
}
