package com.ssn.simulation.plugin.zFTS1;


public class zTG1_WTSK extends zTG1 {

    // Fahrauftrag
    @TelegramField(offset = 66, length = 18)
    protected String Ziel;

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

    @TelegramField(offset = 104, length = 4)
    protected int HU_Höhe;

    @TelegramField(offset = 109, length = 1)
    protected String Prioritätsbit;

    public zTG1_WTSK(String telegrammstring) {
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


    public zTG1_WTSK(ByteHandler byteHandler) {
        super(byteHandler);
    }
}
