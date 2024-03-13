package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.telegrams.Telegram;

import java.io.Serializable;

public class zTG1_INFO extends zTG1 {

    // Information HU aufgenommen

    public static final String TELEGRAM_TYPE = "INFO";

    @TelegramField(offset = 86, length = 4)
    protected String MFS_Error;

    @TelegramField(offset = 66, length = 20)
    protected String HU_Nummer;

    @TelegramField(offset = 90, length = 48)
    protected String Reserve;

    @TelegramField(offset = 48, length = 18)
    protected String Quelle;

    public zTG1_INFO(String telegrammstring) {

        super(telegrammstring);

        this.Quelle = telegrammstring.substring(48, 66);
        this.HU_Nummer = telegrammstring.substring(66, 86);
        this.MFS_Error = telegrammstring.substring(86, 90);
        this.Reserve = telegrammstring.substring(90, 138);

    }

    public zTG1_INFO() {
        super();
        this.telegramsubtype = TELEGRAM_TYPE;
    }

    public static zTG1_INFO getHeaderData() {
        zTG1_INFO inf1 = new zTG1_INFO();
        zTG1.getHeader(inf1);
        inf1.telegramsubtype = zTG1_INFO.TELEGRAM_TYPE;
        return inf1;
    }

    public String convertToString() {
        StringBuilder sb = new StringBuilder();

        // Attribute in gewünschter Reihenfolge anhängen
        sb.append(fillWithDots(this.sender, 8)); // Beispielattribut "Name" mit Länge 10
        sb.append(fillWithDots(this.receiver, 8)); // Beispielattribut "Age" mit Länge 5
        sb.append(fillWithDots(this.CP, 18));
        sb.append(fillWithDots(this.Handshake, 2));
        sb.append(fillWithDots(Integer.toString(this.sequencenumber), 4));
        sb.append(fillWithDots(this.Commerror, 4));
        sb.append(fillWithDots(this.telegramsubtype, 4));
        sb.append(fillWithDots(this.Quelle, 18));
        sb.append(fillWithDots(this.HU_Nummer, 20));
        sb.append(fillWithDots(this.MFS_Error, 4));
        sb.append(fillWithDots(this.Reserve, 48));
        sb.append(fillWithDots(this.Endekennzeichen, 2));

        return sb.toString();
    }

}
