/*
 * RawGazeHandler.java
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
package de.dfki.km.text20.browserplugin.services.mastergazehandler.impl.gazehandler.fixation;

import java.awt.Point;
import java.util.List;

import de.dfki.km.text20.browserplugin.services.mastergazehandler.impl.gazehandler.AbstractGazeHandler;
import de.dfki.km.text20.services.evaluators.gaze.GazeEvaluator;
import de.dfki.km.text20.services.evaluators.gaze.listenertypes.fixation.FixationEvent;
import de.dfki.km.text20.services.evaluators.gaze.listenertypes.fixation.FixationEventType;
import de.dfki.km.text20.services.evaluators.gaze.listenertypes.fixation.FixationListener;
import de.dfki.km.text20.services.pseudorenderer.CoordinatesType;
import de.dfki.km.text20.services.pseudorenderer.PseudorendererStatus;

/**
 * @author rb
 *
 */
public class FixationHandler extends AbstractGazeHandler {

    @SuppressWarnings("boxing")
    protected void handleEvent(final FixationEvent event) {
        if (this.reducedCommunication) return;

        final List<String> handler = this.masterGazeHandler.getHandlerForType("fixation");
        if (handler.size() == 0) return;

        // If the element is not visible, don't do anything
        if (!this.pseudorenderer.getStatus().contains(PseudorendererStatus.VISIBLE))
            return;

        // Has the fixation started?
        if (event.getType() == FixationEventType.FIXATION_START) {
            final Point dp = this.pseudorenderer.convertPoint(event.getFixation().getCenter(), CoordinatesType.SCREEN_BASED, CoordinatesType.DOCUMENT_BASED);
            if (dp == null) return;

            // Execute all handler
            for (final String h : handler) {
                this.browserPlugin.executeJSFunction(h, dp.x, dp.y);
            }
        }

        // Or ended?
        if (event.getType() == FixationEventType.FIXATION_END) {
            // Execute all handler
            for (final String h : handler) {
                this.browserPlugin.executeJSFunction(h, -1, -1);
            }
        }
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.services.mastergazehandler.impl.gazehandler.AbstractGazeHandler#registerToEvaluator(de.dfki.km.augmentedtext.services.gazeevaluator.GazeEvaluator)
     */

    @Override
    protected void registerToEvaluator(final GazeEvaluator evaluator) {
        evaluator.addGazeEvaluationListener(new FixationListener() {

            public void newGazeEvaluationEvent(final FixationEvent event) {
                handleEvent(event);
            }
        });
    }
}