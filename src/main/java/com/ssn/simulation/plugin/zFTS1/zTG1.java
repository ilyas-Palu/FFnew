package com.ssn.simulation.plugin.zFTS1;

import com.ssn.simulation.telegrams.Telegram;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class zTG1 implements Telegram, Serializable {

    // implementiert den Telegramm Header für alle FTS Telegrammtypen

    public static final String TELEGRAM_DELIMITER_START = "E";
    public static final String TELEGRAM_DELIMITER_START_ALL = "EWMMFS";
    public static final String TELEGRAM_DELIMITER_END_1 = "#";
    public static final String TELEGRAM_DELIMITER_END_2 = "##";
    public static final String TELEGRAM_DELIMITER_HEADER1 = "MFSEWM..";
    public static final String TELEGRAM_DELIMITER_HEADER2 = "TOMPROD.";
    public static final String TELEGRAM_DELIMITER_ENDING = "##";

    public static final String Handshake1 = "D";
    public static final String Handshake2 = "Q";

    public ByteHandler byteHandler;

    protected String telegramtyp;

    protected String stringtelegram;

    @TelegramField(offset = 0, length = 8)
    protected String sender;

    @TelegramField(offset = 8, length = 8)
    protected String receiver;

    @TelegramField(offset = 16, length = 18)
    protected String CP;

    @TelegramField(offset = 34, length = 2)
    protected String Handshake;

    @TelegramField(offset = 36, length = 4)
    protected int sequencenumber;

    @TelegramField(offset = 40, length = 4)
    protected String Commerror;

    @TelegramField(offset = 44, length = 4)
    protected String telegramsubtype;

    // protected zTG1 header;

    // gemeinsame spezfische Nutzdatenfelder
    @TelegramField(offset = 138, length = 2)
    protected String Endekennzeichen;

    // Ableitungen Weaseltelegramm

    protected byte telegramType;
    protected int tsn;
    protected String fleetId;
    protected String FTFId;
    protected boolean assigned;
    protected byte[] confarray;
    public Boolean confirmed;

    protected zTG1(String telegrammstring) {
        telegramtyp = "FTS_TG";
        confirmed = false;

        // IF Prüfung, dass String 140 Zeichen hat / Byt

        // evtl Prüfung Sequenznummer inlusive Singleton Umsetzung

        stringtelegram = telegrammstring;

        sender = telegrammstring.substring(0, 8);
        receiver = telegrammstring.substring(8, 16);
        CP = telegrammstring.substring(16, 34);
        Handshake = telegrammstring.substring(34, 36);
        sequencenumber = Integer.parseInt(telegrammstring.substring(36, 40));
        Commerror = telegrammstring.substring(40, 44);
        telegramsubtype = telegrammstring.substring(44, 48);

        // header.stringtelegram = telegrammstring;
        Endekennzeichen = telegrammstring.substring(138, 140);

    }

    protected zTG1() {
        telegramtyp = "FTS_TG";
        confirmed = false;
    }

    public zTG1(ByteHandler byteHandler) {
        this.byteHandler = byteHandler;
    }

    public static zTG1 interpret(String telegrammstring) {

        // IF Prüfung, dass String 140 Zeichen hat / Byt

        // evtl Prüfung Sequenznummer inlusive Singleton Umsetzung

        String Telegramsubtypecheck = telegrammstring.substring(44, 48);

        switch (Telegramsubtypecheck) {
            case "WTSK":
                return new zTG1_WTSK(telegrammstring);

            case "WTCO":
                return new zTG1_WTCO(telegrammstring);

            case "POSO":
                return new zTG1_POSO(telegrammstring);

            case "INFO":
                return new zTG1_INFO(telegrammstring);

            case "LIFE":
                return new zTG1_LIFE(telegrammstring);
        }

        return null;

    }

    /*
     * public byte[] getByteOutputStream() throws Exception {
     * ByteArrayOutputStream stream = new ByteArrayOutputStream();
     * ByteArrayOutputStream result = new ByteArrayOutputStream();
     * write(stream, 1, telegramType);
     * writeAndfillUpWithZero(stream, 5, tsn);
     * writeAndfillUpWithBlanks(stream, 5, fleetId);
     * writeAndfillUpWithBlanks(stream, 5, FTPId);
     * fillBytes(stream);
     * write(result, 1, WeaselTelegram.TELEGRAM_DELIMITER_START);
     * checksum = getCrc16(stream.toByteArray());
     * writeAndfillUpWithZero(result, 5, checksum);
     * write(result, stream.toByteArray());
     * write(result, 1, WeaselTelegram.TELEGRAM_DELIMITER_END);
     * return result.toByteArray();
     * }
     */

    public static final void write(ByteArrayOutputStream stream, int bytes, Object value) throws IOException {
        stream.write(ByteBuffer.allocate(bytes).put(value.toString().getBytes()).array());
    }



    public String getFTFId() {
        return FTFId;
    }

    public void setFTFId(String weaselId) {
        this.FTFId = weaselId;
    }

    public void setAssigned(boolean b) {
        this.assigned = b;
    }

    // Neue statische Methode Sender und Empfänger tauschen um Quittierungstelegramm
    // zu erhalten

    public void getConfirmation() {
        // Abklären ob Felder des Subtyps verloren gehen, und wann Erstellung und
        // Rückgabe Sinn machen -> Durch reine Stringverwendung gelöst

        try {
            byte[] byteArray = this.stringtelegram.getBytes("UTF-8");

            byte[] temp = new byte[8];

            System.arraycopy(byteArray, 0, temp, 0, 8);

            // Kopieren der Bytes von 9 bis 16 an die Positionen 0 bis 7 im ursprünglichen
            // Byte-Array
            System.arraycopy(byteArray, 8, byteArray, 0, 8);

            // Kopieren der Bytes aus dem temporären Byte-Array an die Positionen 8 bis 15
            // im ursprünglichen Byte-Array
            System.arraycopy(temp, 0, byteArray, 8, 8);

            this.confarray = byteArray;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    // Methode zur Auffüllung mit Punkten bis zur gewünschten Länge
    protected static String fillWithDots(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        int diff = length - value.length();
        for (int i = 0; i < diff; i++) {
            sb.append(".");
        }
        return sb.toString();
    }

    public static zTG1 getHeader(zTG1 obj) {
        obj.sender = zTG1.TELEGRAM_DELIMITER_HEADER2;
        obj.receiver = zTG1.TELEGRAM_DELIMITER_HEADER1;
        obj.CP = null;
        obj.Handshake = "D.";
        obj.sequencenumber = 0;
        obj.Commerror = "";
        return obj;

    }

    public String getTelegramtyp() {
        return telegramtyp;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getCP() {
        return CP;
    }

    public String getHandshake() {
        return Handshake;
    }

    public int getSequencenumber() {
        return sequencenumber;
    }

    public String getCommerror() {
        return Commerror;
    }

    public String getTelegramsubtype() {
        return telegramsubtype;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setCP(String cP) {
        CP = cP;
    }

    public void setHandshake(String handshake) {
        Handshake = handshake;
    }

    public void setSequencenumber(int sequencenumber) {
        this.sequencenumber = sequencenumber;
    }

    public void setCommerror(String commerror) {
        Commerror = commerror;
    }

    public void setTelegramsubtype(String telegramsubtype) {
        this.telegramsubtype = telegramsubtype;
    }
    // in Bytearray für Output

    public boolean isAckTel() {
        if (this.Handshake.equals(Handshake2)) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean isLife() {
        if (this.telegramsubtype.equals("LIFE")) {
            return true;
        }
        return false;

    };

    public void setHeaderfields(zTG1 header) {
        this.sender = header.sender;
        this.receiver = header.receiver;
        this.Handshake = header.Handshake;
        this.sequencenumber = header.sequencenumber;
        this.Endekennzeichen = header.Endekennzeichen;
        this.Commerror = header.Commerror;
        this.telegramsubtype = header.telegramsubtype;
    }
}
