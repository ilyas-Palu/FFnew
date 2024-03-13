package com.ssn.simulation.plugin.zFTS1;


public class zTG1_LIFE extends zTG1 {

    // Aufrechterhaltung Kommunikationsverbindung

    @TelegramField(offset = 48, length = 90)
    protected String Reserve;

    public zTG1_LIFE(String telegrammstring) {

        super(telegrammstring);

        this.Reserve = telegrammstring.substring(48, 138);

    }

    public zTG1_LIFE(ByteHandler byteHandler) {
        super(byteHandler);
    }


}
