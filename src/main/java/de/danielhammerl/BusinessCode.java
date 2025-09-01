package de.danielhammerl;

import de.danielhammerl.datastructs.subsystem0.DataStructSubsystem0;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;

public class BusinessCode {
    void update(Connection connection, final Subsystem[] subsystems) throws RPCException {
        // receive data from krpc
        SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
        SpaceCenter.Vessel vessel = spaceCenter.getActiveVessel();
        SpaceCenter.Flight flight = vessel.flight(vessel.getReferenceFrame());
        double altitude = flight.getSurfaceAltitude();

        DataStructSubsystem0 dataStructSubsystem0 = new DataStructSubsystem0();
        // receive data from subsystems
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getState()) {
                subsystem.getData().ifPresent(dataStructSubsystem0::fromBytes);
            }
        }

        // calculate;
        dataStructSubsystem0.getToWrite().altitude = altitude;

        // send data to krpc

        // send data to subsystems
        subsystems[0].setData(dataStructSubsystem0);
    }
}
