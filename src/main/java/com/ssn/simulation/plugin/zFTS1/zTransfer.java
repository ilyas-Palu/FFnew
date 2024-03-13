package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.core.Event;

public class zTransfer extends Event {

    public Entity Conveyor;

    public zFTS_Entity1 FTF;

    public long endTime;

    public String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public zTransfer(long time) {
        super(time);
    }

    public Entity getConveyor() {
        return Conveyor;
    }

    public void setConveyor(Entity conveyor) {
        Conveyor = conveyor;
    }

    public zFTS_Entity1 getFTF() {
        return FTF;
    }

    public void setFTF(zFTS_Entity1 fTF) {
        FTF = fTF;
    }

    @Override
    public void onEvent() {
        if (this.Conveyor == this.FTF.destMach) {
            FTF.handleDestTransfer(endTime);
        } else {
            FTF.handleSrcTransfer(endTime, value);
        }
    };
}
