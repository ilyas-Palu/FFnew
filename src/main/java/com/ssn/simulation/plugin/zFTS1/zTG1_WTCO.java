package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.telegrams.Telegram;

import java.io.Serializable;

public class zTG1_WTCO extends zTG1 {

    // Quittierung Fahrauftrag

    // Fahrauftrag
    @TelegramField(offset = 66, length = 18)
    protected String Ziel;

    @TelegramField(offset = 104, length = 4)
    protected int HU_Höhe;

    @TelegramField(offset = 108, length = 1)
    protected String Paarbit;

    @TelegramField(offset = 110, length = 4)
    protected String MFS_Error;

    @TelegramField(offset = 84, length = 20)
    protected String HU_Nummer;

    @TelegramField(offset = 114, length = 24)
    protected String Reserve;

    @TelegramField(offset = 48, length = 18)
    protected String Quelle;

    @TelegramField(offset = 109, length = 1)
    protected String Prioritätsbit;

    public static final String TELEGRAM_TYPE = "WTCO";

    public zTG1_WTCO(String telegrammstring) {

        super(telegrammstring);

        this.Quelle = telegrammstring.substring(48, 66);
        this.Ziel = telegrammstring.substring(66, 84);
        this.HU_Nummer = telegrammstring.substring(84, 104);
        this.HU_Höhe = Integer.parseInt(telegrammstring.substring(104, 108));
        this.Paarbit = telegrammstring.substring(108);
        this.Prioritätsbit = telegrammstring.substring(109);
        this.MFS_Error = telegrammstring.substring(110, 114);
        this.Reserve = telegrammstring.substring(114, 138);

    }

    public zTG1_WTCO() {
        super();
    }

    public static zTG1_WTCO getHeaderData() {
        zTG1_WTCO wtco1 = new zTG1_WTCO();
        zTG1.getHeader(wtco1);
        wtco1.telegramsubtype = zTG1_WTCO.TELEGRAM_TYPE;
        return wtco1;
    }

    public String convertToString() {
        StringBuilder sb = new StringBuilder();

        // Attribute in gewünschter Reihenfolge anhängen
        sb.append(fillWithDots(this.sender, 10)); // Beispielattribut "Name" mit Länge 10
        sb.append(fillWithDots(this.receiver, 5)); // Beispielattribut "Age" mit Länge 5
        sb.append(fillWithDots(this.CP, 5));
        sb.append(fillWithDots(this.Handshake, 5));
        sb.append(fillWithDots(Integer.toString(this.sequencenumber), 5));
        sb.append(fillWithDots(this.Commerror, 5));
        sb.append(fillWithDots(this.telegramsubtype, 5));
        sb.append(fillWithDots(this.Quelle, 5));
        sb.append(fillWithDots(this.HU_Nummer, 5));
        sb.append(fillWithDots(this.MFS_Error, 5));
        sb.append(fillWithDots(this.Reserve, 5));
        sb.append(fillWithDots(this.Endekennzeichen, 5));

        return sb.toString();
    }
}
