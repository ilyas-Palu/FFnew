package com.ssn.simulation.plugin.zFTS1;


public class zTG1_POSO extends zTG1 {

    // Positionierungsauftrag
    @TelegramField(offset = 86, length = 4)
    protected int HU_Höhe;

    @TelegramField(offset = 90, length = 4)
    protected String MFS_Error;

    @TelegramField(offset = 66, length = 20)
    protected String HU_Nummer;

    @TelegramField(offset = 94, length = 34)
    protected String Reserve;

    @TelegramField(offset = 48, length = 18)
    protected String Quelle;

    public zTG1_POSO(String telegrammstring) {
        super(telegrammstring);
        this.Quelle = telegrammstring.substring(48, 66);
        this.HU_Nummer = telegrammstring.substring(66, 86);
        this.HU_Höhe = Integer.parseInt(telegrammstring.substring(86, 90));
        this.MFS_Error = telegrammstring.substring(90, 94);
        this.Reserve = telegrammstring.substring(94, 138);

    }

    public zTG1_POSO(ByteHandler byteHandler) {
        super(byteHandler);
    }
}
