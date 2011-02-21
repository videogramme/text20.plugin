/*
 * EyeTrackingDevice.java
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
package de.dfki.km.text20.services.trackingdevices.eyes;

import de.dfki.km.text20.services.trackingdevices.common.TrackingDevice;
import de.dfki.km.text20.trackingserver.eyes.remote.TrackingCommand;
import de.dfki.km.text20.trackingserver.eyes.remote.options.SendCommandOption;

/**
 * Represents a tracking device.
 * 
 * @author rb 
 */
public interface EyeTrackingDevice extends
        TrackingDevice<EyeTrackingDeviceInfo, EyeTrackingEvent, EyeTrackingListener> {

    /**
     * Returns the type of the device.
     * 
     * @return .
     */
    public EyeTrackingDeviceType getDeviceType();

    /**
     * Sends a low level tracking command to the device
     * 
     * @param command
     * @param options
     */
    public void sendLowLevelCommand(TrackingCommand command, SendCommandOption... options);
}
