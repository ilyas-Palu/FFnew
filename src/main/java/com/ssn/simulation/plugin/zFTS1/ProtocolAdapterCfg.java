package com.ssn.simulation.plugin.zFTS1;

public class ProtocolAdapterCfg {
    protected long waitForAckTimeout = 5000;
    protected long lifeTelegramInterval = 0;

    public long getLifeTelegramInterval() {
        return lifeTelegramInterval;
    }

    public void setLifeTelegramInterval(long lifeTelegramInterval) {
        this.lifeTelegramInterval = lifeTelegramInterval;
    }

    public long getWaitForAckTimeout() {
        return waitForAckTimeout;
    }

    public void setWaitForAckTimeout(long waitForAckTimeout) {
        this.waitForAckTimeout = waitForAckTimeout;
    }
}
