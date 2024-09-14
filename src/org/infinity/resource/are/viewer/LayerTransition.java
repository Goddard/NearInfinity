// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;
import org.infinity.util.Logger;

/**
 * Manages map transition layer objects.
 */
public class LayerTransition extends BasicLayer<LayerObjectTransition, AreResource> {
  private static final String AVAILABLE_FMT = "Map transitions: %d";

  public LayerTransition(AreResource are, AreaViewer viewer) {
    super(are, ViewerConstants.LayerType.TRANSITION, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer() {
    final List<LayerObjectTransition> list = getLayerObjects();
    for (int i = 0; i < LayerObjectTransition.FIELD_NAME.length; i++) {
      ResourceRef ref = (ResourceRef) parent.getAttribute(LayerObjectTransition.FIELD_NAME[i]);
      if (ref != null && !ref.isEmpty()) {
        try {
          final AreResource destAre = new AreResource(ResourceFactory.getResourceEntry(ref.getResourceName()));
          LayerObjectTransition obj = new LayerObjectTransition(parent, destAre, i, getViewer().getRenderer());
          setListeners(obj);
          list.add(obj);
        } catch (Exception e) {
          Logger.error(e);
        }
      }
    }
    setInitialized(true);
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }
}
