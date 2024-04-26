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
        if( this.order instanceof zTG1_WTSK){
            zTG1_WTSK TWTSK = (zTG1_WTSK) order;
            this.controller.useUnutiliziedFTFwtsk(TWTSK);
        }
        else{
            zTG1_POSO TPOSO = (zTG1_POSO) order;
            this.controller.handlePOSO(TPOSO);
        }
        
    }

}
