package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.core.Event;

public class zDelay extends Event {

    public zTG1 order;

    public zFTS1 controller;

    public zDelay(long time, zFTS1 controller, zTG1 order) {
        super(time);
        this.controller = controller;
        this.order = order;
    }

    @Override
    public void onEvent() {
        controller.useUnutiliziedFTFtg(order);

    }

}
