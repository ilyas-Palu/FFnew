package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.core.Event;

public class zDelay extends Event {

    public zTG1_WTSK wtsk;

    public zFTS1 controller;

    public zDelay(long time, zFTS1 controller, zTG1_WTSK wtsk) {
        super(time);
        this.controller = controller;
        this.wtsk = wtsk;
    }

    @Override
    public void onEvent() {
        this.controller.useUnutiliziedFTF(wtsk);
    }

}
