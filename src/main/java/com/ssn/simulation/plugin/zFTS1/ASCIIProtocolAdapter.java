package com.ssn.simulation.plugin.zFTS1;

import java.nio.charset.StandardCharsets;

import com.ssi.wasoc.api.telegram.InboundTelegram;
import com.ssi.wasoc.internal.nio.protocol.ProtocolAdapter;
import com.ssi.wasoc.internal.nio.protocol.ProtocolAdapterState;
import com.ssi.wasoc.internal.nio.protocol.ProtocolError;
import com.ssi.wasoc.internal.nio.protocol.filter.ByteStreamTelegramFilter;

public class ASCIIProtocolAdapter implements ProtocolAdapter {

    protected ByteHandler byteHandler;
    protected ProtocolAdapterCfg config;
    protected LengthTelegramFilter telegramFilter;

    public ASCIIProtocolAdapter(ByteHandler byteHandler, ProtocolAdapterCfg config) {
        this.byteHandler = byteHandler;
        this.config = config;
        this.telegramFilter = new LengthTelegramFilter(byteHandler.getTelegramLength());
    }

    @Override
    public ProtocolError checkAliveTelegram(byte[] tel, byte[] currentlyHandledTel) {
        return null;
    }

    @Override
    public ProtocolError checkDataTelegram(byte[] tel, byte[] currentlyHandledTel) {
        return null;
    }

    @Override
    public void confirmTelegramHandled() {
    }

    @Override
    public byte[] createAckTelegram(byte[] recTel) {
        var tele = this.extractHeaderFields(recTel);
        if (tele == null) {
            return null;
        }
        var sender = tele.getSender();
        tele.setSender(tele.getReceiver());
        tele.setReceiver(sender);
        tele.setHandshake("Q");
        try {
            this.byteHandler.write(tele, recTel);
        } catch (ByteWriteException e) {
            System.err.println("exception in createAckTelegram: " + e.getMessage() + "\n" + e.getStackTrace());
            return null;
        }
        return recTel;
    }

    @Override
    public byte[] createAliveTelegram() {
        var life = new zTG1_LIFE(this.byteHandler);
        var telegram = this.byteHandler.createTelegram();
        try {
            this.byteHandler.write(life, telegram);
        } catch (ByteWriteException e) {
            return null;
        }
        return telegram;
    }

    @Override
    public byte[] createNAckTelegram(byte[] recTel, ProtocolError errorCode) {
        return null;
    }

    @Override
    public String format(byte[] tel) {
        return new String(tel, StandardCharsets.US_ASCII);
    }

    @Override
    public long getDisconnectIdleTime() {
        return 0;
    }

    @Override
    public ProtocolAdapterState getProtocolState() {
        return ProtocolAdapterState.STATELESS;
    }

    @Override
    public long getSendAliveTelegramRate() {
        return this.config.getLifeTelegramInterval();
    }

    @Override
    public ByteStreamTelegramFilter getTelegramFilter() {
        return this.telegramFilter;
    }

    @Override
    public long getWaitForAckBeforeRepeatTimeout(byte[] outgoingTel) {
        return this.config.waitForAckTimeout;
    }

    @Override
    public boolean isAckTelegram(byte[] tel) {
        var tele = this.extractHeaderFields(tel);
        return tele != null && tele.isAckTel();
    }

    @Override
    public boolean isAliveTelegram(byte[] msg) {
        var tele = this.extractHeaderFields(msg);
        return tele != null && tele.isLife();
    }

    @Override
    public boolean isDataTelegram(byte[] tel) {
        var tele = this.extractHeaderFields(tel);
        return tele != null && !tele.isAckTel();
    }

    @Override
    public boolean isMatchingAckTelegram(byte[] sendTel, byte[] ack) {
        var snd = this.extractHeaderFields(sendTel);
        var rcv = this.extractHeaderFields(ack);
        return snd != null && rcv != null && snd.getSequencenumber() == rcv.getSequencenumber();
    }

    @Override
    public boolean isNAckTelegram(byte[] tel) {
        return false;
    }

    @Override
    public boolean isWaitingForAckAfterSend(byte[] outgoingTel) {
        return true;
    }

    @Override
    public InboundTelegram prepareTelegram(byte[] tel) {
        return new InboundTelegram(tel, null);
    }

    @Override
    public void reset() {
    }

    @Override
    public byte[] wrapWithHeader(byte[] payload) {
        return payload;
    }

    public zTG1 extractHeaderFields(byte[] data) {
        try {
            return this.byteHandler.read(data, zTG1.class);
        } catch (ByteReadException e) {
            System.err.println("exception in extractHeaderFields: " + e.getMessage() + "\n" + e.getStackTrace());
            return null;
        }
    }

}