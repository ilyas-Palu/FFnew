package com.ssn.simulation.plugin.zFTS1;

import java.util.Map;
import java.util.Map.Entry;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.core.Event;

public class zPaarbit extends Event {

    public zTG1_WTSK paarbitTG;

    public zFTS1 controller;

    public zPaarbit(long time, zFTS1 controller, zTG1_WTSK wtsk) {
        super(time);
        this.controller = controller;
        this.paarbitTG = wtsk;
    }

    @Override
    public void onEvent() {
        this.controller.inspectPaarbit(paarbitTG);
    }

}
