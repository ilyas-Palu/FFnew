package com.ssn.simulation.plugin.zFTS1;

import com.ssi.wasoc.internal.nio.protocol.filter.ByteStreamTelegramFilter;
import com.ssi.wasoc.internal.nio.protocol.filter.TelegramFilterResult;

public class LengthTelegramFilter implements ByteStreamTelegramFilter {

    protected int length;

    public LengthTelegramFilter(int length) {
        this.length = length;
    }

    @Override
    public TelegramFilterResult extractNextTelegram(byte[] bytes, int offset, int length) {
        return length < this.length ? null : new TelegramFilterResult(offset, this.length);
    }
    
}