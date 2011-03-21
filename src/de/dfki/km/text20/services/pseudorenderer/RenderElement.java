/*
 * RenderElement.java
 * 
 * Copyright (c) 2010, Ralf Biedert, DFKI. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA 02110-1301  USA
 *
 */
package de.dfki.km.text20.services.pseudorenderer;

import java.awt.Rectangle;
import java.io.Serializable;

/**
 * Represents a rendering element. Note that a render element is a 'live' object. Its
 * values might change after you query it, depending on changes to the element or the
 * pseudorenderer.
 * 
 * @author Ralf Biedert
 * 
 */
public interface RenderElement {

    /**
     * Returns the coordinates type this element conforms to.
     * 
     * @return The {@link CoordinatesType} of this element
     */
    public CoordinatesType getCoordinatesType();

    /**
     * Returns the position of this element relative to the given type of coordinate
     * system.
     * 
     * @param type Specified relative to which coordinates system we should return
     * coordinates.
     * 
     * @return .
     */
    public Rectangle getGeometry(CoordinatesType type);

    /**
     * Gets the identifier of that element which is used outside.
     * 
     * @return .
     */
    public String getIdentifier();

    /**
     * Returns a meta attribute value.
     * 
     * @param key
     * @return .
     */
    public Serializable getMetaAttribute(RenderElementMetaAttribute key);

    /**
     * Returns the element's z index.
     * 
     * @return .
     */
    public int getZIndex();

    /**
     * Checks if the render element has the given key.
     * 
     * @param key The key to check.
     * 
     * @return True if it has the key, false if not.
     */
    public boolean hasMetaAttribute(RenderElementMetaAttribute key);

    /**
     * Returns true if the element is visible.
     * 
     * @return True if the element is visible, false if not.
     */
    public boolean isVisible();

    /**
     * Sets the windowGeometry of this element. Be careful! The type parameter influences
     * how these elements are treated later on, it is NOT merely a coordinate
     * transformation.<br/>
     * <br/>
     * 
     * Elements with a DOCUMENT_POSITION don't change their position relative to the
     * document start.<br/>
     * <br/>
     * 
     * Elements with a VIEWPORT_POSITION "move" their document position when the window
     * scrolls.<br/>
     * <br/>
     * 
     * @param rectangle
     * @param type
     * 
     */
    public void setGeometry(Rectangle rectangle, CoordinatesType type);

    /**
     * Sets the identifier of that element which is used outside.
     * 
     * @param id The element's id, mostly the id used in the DOM tree.
     */
    public void setIdentifier(String id);

    /**
     * Sets a meta attribute.
     * 
     * @param key
     * @param value
     */
    public void setMetaAttribute(RenderElementMetaAttribute key, Serializable value);

    /**
     * Sets if this element is visible or not.
     * 
     * @param visible True if it is, false if not.
     */
    public void setVisible(boolean visible);

    /**
     * Sets a z index.
     * 
     * @param zindex
     */
    public void setZIndex(int zindex);
}
