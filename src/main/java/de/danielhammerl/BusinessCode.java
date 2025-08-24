package de.danielhammerl;

import krpc.client.services.KRPC;

public class BusinessCode {
    void update(KRPC krpc, final Subsystem[] subsystems) {
        // receive data from krpc

        // receive data from subsystems
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getState()) {

            }
        }

        // calculate

        // send data to krpc

        // send data to subsystems
    }
}
