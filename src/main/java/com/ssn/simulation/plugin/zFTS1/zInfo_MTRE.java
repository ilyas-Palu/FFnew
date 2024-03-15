package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.core.Event;

public class zInfo_MTRE extends Event {

    public Entity Conveyor;

    public zFTS_Entity1 FTF;

    public long endTime;

    public String process;

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public zInfo_MTRE(long time) {
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

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public void onEvent() {
        this.FTF.handleSrcTransfer(this.endTime, process);

    };
}
