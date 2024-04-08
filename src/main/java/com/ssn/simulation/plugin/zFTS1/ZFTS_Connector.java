package com.ssn.simulation.plugin.zFTS1;

import java.awt.Color;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ssi.wasoc.WaSoc;
import com.ssi.wasoc.api.WaSocConnectionRegistry;
import com.ssi.wasoc.api.WaSocTelegramHandler;
import com.ssi.wasoc.api.executor.WaSocThreadPoolExecutorFactory;
import com.ssi.wasoc.api.telegram.InboundTelegram;
import com.ssi.wasoc.api.telegram.OutboundTelegram;
import com.ssn.simulation.core.Entity;
import com.ssn.simulation.editor.ValidationMessage;
import com.ssn.simulation.entities.AntSimSocConfig;
import com.ssn.simulation.events.TelegramEvent;

public class ZFTS_Connector extends Entity implements WaSocTelegramHandler {

    @AntSimSocConfig
    protected String controllerIp;

    @AntSimSocConfig
    protected int controllerPort;

    protected String fts_Sender_Id;

    @JsonIgnore
    protected volatile transient LinkedBlockingQueue<zTG1> telegrams;
    @JsonIgnore
    protected volatile transient Map<String, Entity> handlers;
    @JsonIgnore
    protected volatile transient Map<Integer, zTG1> sequencecheck;
    @JsonIgnore
    protected volatile transient Map<Integer, zFTS1> serials;
    @JsonIgnore
    protected volatile transient Map<String, zFTS1> Fleets;
    @JsonIgnore
    protected volatile transient Map<Integer, Socket> connections;
    @JsonIgnore
    protected volatile transient Set<zFTS1> FTS_Controller;
    @JsonIgnore
    protected volatile transient boolean runtime;
    @JsonIgnore
    protected volatile transient boolean connecting;
    @JsonIgnore
    protected volatile transient long lastWaitingTime;
    @JsonIgnore
    protected volatile transient String fleetId;
    @JsonIgnore
    protected volatile transient zFTS1 ControllerFTS;
    @JsonIgnore
    public ByteHandler byteHandler;
    @JsonIgnore
    private WaSocThreadPoolExecutorFactory executorFactory;
    @JsonIgnore
    private WaSocConnectionRegistry connectionRegistry;

    @JsonIgnore
    public int sqn;

    public ZFTS_Connector() {

        super();
        this.sizex = 1;
        this.sizey = 1;
        this.sizez = 1;
        this.controllerPort = 8888;
        this.fleetId = "4444";
        this.ControllerFTS = null;
    }

    @Override
    public String getCategory() {
        return "FTS";
    }

    @Override
    public void onAttributesChanged() {
        super.onAttributesChanged();
        setStringProperty("controllerIp", controllerIp, "Connector");
        setIntegerProperty("controlerPort", controllerPort, "Connector");
        setStringProperty("FTS_Sender_ID", fts_Sender_Id, "Connector");
    }

    @Override
    public void onPropertiesChanged() {
        this.controllerIp = getStringProperty("controllerIp");
        this.controllerPort = getIntegerProperty("controlerPort");
        this.fts_Sender_Id = getStringProperty("FTS_Sender_ID");
        super.onPropertiesChanged();
    }

    @Override
    public Object clone() {
        try {
            ZFTS_Connector entity = (ZFTS_Connector) super.clone();
            return entity;
        } catch (Exception e) {
            core.logError(this, "unable to clone entity: " + e.toString(), e);
            return null;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnterRuntimeMode() {
        this.runtime = true;
        connect();
    }

    @Override
    public void onExitRuntimeMode() {
        this.runtime = false;
        disconnect();
    }

    @Override
    public void onNotify() {
        // checkDistances();
        // checkSegmentGroups();

        while (!telegrams.isEmpty()) {
            zTG1 telegram = telegrams.poll(); // Änderung auf zTG1
            core.logInfo(this, "neues Telegramm erkannt");
            if (!serials.isEmpty()) {
                // Änderung durchgeführt , Herausziehen der Controller Entität,
                // sichern per FleetID Vergleich ( evtl. noch einbauen für Erwarteirbarkeit)
                if (telegram != null) {

                    TelegramEvent event = new TelegramEvent();
                    event.setTime(core.now());
                    event.setEntity(ControllerFTS);
                    event.setTelegram(telegram);
                    core.addEvent(event);

                    core.logInfo(this, "Telegramm wird weitergeleitet an Controller");
                } else {
                    core.logError(this, "handler not found for " + telegram);
                }

            }
        }
    };

    @Override
    public void onReset() {
        super.onReset();
        this.telegrams = new LinkedBlockingQueue<>();
        this.handlers = new HashMap<>();
        this.serials = new HashMap<>();
        this.connections = new HashMap<>();
        this.FTS_Controller = new HashSet<>();
        this.connecting = false;
        this.sqn = 0;
        for (Entity entity : core.getEntities()) {
            String key = entity.getTelegramHandlerId();
            if (key != null) {
                handlers.put(key, entity);
            }
            if (entity instanceof zFTS1) {
                zFTS1 fts = (zFTS1) entity;
                serials.put(Integer.parseInt(fts.getFleetID()), fts);
                FTS_Controller.add(fts);
                this.ControllerFTS = fts; // normale Abspeicherung
            }
        }
        this.lastWaitingTime = 0;
        core.addNotifier(this); // ergänzt, wichtige Funktion! Anstim Standard
    }

    @Override
    public Color getBackgroundColor() {
        if (connections.isEmpty()) {
            return core.entityIdleColor;
        } else {
            return core.entityOccupiedColor2;
        }
    }

    // @Override
    // onDraw

    @Override
    public void onValidate(List<ValidationMessage> results) {
        super.onValidate(results);
        for (Entity entity : core.getEntities()) {
            String key = entity.getTelegramHandlerId();
            if (key != null && !key.equals("")) {
                Entity other = handlers.get(key);
                if (other != null && other != entity) {
                    results.add(new ValidationMessage(this, "connector",
                            "multiple handler key " + key + ": " + entity + " and " + other));
                }
            }
        }
        if (this.controllerIp.equals("")) {
            results.add(new ValidationMessage(this, "controller", "controller ip-address is not configured"));
        }
        if (this.controllerPort == 0) {
            results.add(new ValidationMessage(this, "controller", "controller port is not configured"));
        }
    }

    // TELEGRAMMVERARBEITUNG
    // -------------------------------------

    public synchronized void sendTelegram(zTG1 telegram) {
        try {
            telegram.setSender(this.checkSenderId(this.fts_Sender_Id)); // Ktech Auslagerung variabler sender);
            telegram.setReceiver(zTG1.TELEGRAM_DELIMITER_HEADER1); // unsicher cn1
            if (sqn == 9999) {
                sqn = 1;
            } else {
                sqn += 1;
            }
            telegram.setSequencenumber(sqn);
            telegram.setHandshake(zTG1.Handshake1);
            var connection = this.connectionRegistry.getConnection(zTG1.TELEGRAM_DELIMITER_START_ALL);
            var bytes = this.byteHandler.createTelegram();
            this.byteHandler.write(telegram, zTG1.class, bytes);
            this.byteHandler.write(telegram, bytes);
            connection.sendTelegramAsync(new OutboundTelegram(bytes));
            this.core.logSend(telegram);
        } catch (Exception e) {
            this.logError("failed to send " + telegram.getTelegramsubtype() + " telegram: " + e.getMessage());
        }
    }

    public void receiveTelegram(zTG1 telegram) {
        core.logReceive(telegram);
        core.logInfo(this, "receives a FTF telegram: " + telegram.toString());
        try {
            telegrams.put(telegram);
            core.logInfo(this, "new telegram successfully added ");
        } catch (Exception e) {
            core.logError(this, "Exception when trying to add new telegram: " + e);

        }

    }

    public void connect() {

        this.core.logInfo(this, "open server on port " + this.controllerPort);
        try {
            this.byteHandler = new ByteHandler((byte) ('.'), 140,
                    ("##").getBytes());
            this.executorFactory = WaSoc.createDefaultExecutorFactory();
            this.connectionRegistry = WaSoc.createSocketConnectionRegistry(this.executorFactory);
            var cfg = new ASCIIConnectionCfg(zTG1.TELEGRAM_DELIMITER_START_ALL, controllerIp, this.controllerPort, true,
                    byteHandler, new ProtocolAdapterCfg());
            this.connectionRegistry.configureConnection(cfg, this);
            this.executorFactory.startAll();
        } catch (Exception e) {
            this.core.logError(this, "unable to connect: " + e.toString(), e);
        }
        this.core.logInfo(this, "ro-ber connected");

    }

    public void disconnect() {
        executorFactory.stopAll();
    }

    public LinkedBlockingQueue<zTG1> getTelegrams() {
        return telegrams;
    }

    @Override
    public boolean handleTelegram(InboundTelegram telegram) {
        try {
            var header = this.byteHandler.read(telegram.getPayload(), zTG1.class);
            if ((header.getSequencenumber() > 0 && header.getSequencenumber() == this.sqn
                    && header.getSequencenumber() < this.sqn && this.sqn != 9999)
                    || (this.sqn == 9999 && header.getSequencenumber() > 1)) {

                this.core.logDebug(this,
                        "telegram with seqno " + header.getSequencenumber()
                                + " already processed -> ignoring telegram");
                return true;
            }
            this.sqn = header.getSequencenumber();
            switch (header.getTelegramsubtype()) {
                case "WTSK":
                    zTG1_WTSK wtsk = byteHandler.read(telegram.getPayload(), zTG1_WTSK.class);
                    wtsk.setHeaderfields(header);
                    byteHandler.read(telegram.getPayload(), wtsk);
                    this.receiveTelegram(wtsk);
                    break;
                case "POSO":
                    zTG1_POSO poso = byteHandler.read(telegram.getPayload(), zTG1_POSO.class);
                    poso.setHeaderfields(header);
                    byteHandler.read(telegram.getPayload(), poso);
                    this.receiveTelegram(poso);
                    break;
            }
            ;
        } catch (ByteReadException e) {
            core.logError(this, "Exception bei Wasoc Telegramm Hanlding : " + e);
        }

        return true;

    }

    public String getFts_Sender_Id() {
        return fts_Sender_Id;
    }

    public String checkSenderId(String str) {
        if (str != null && str.length() < 9 && str.length() > 0) {
            return str;
        } else {
            core.logInfo(this, "No usable SenderId in Controller Field, default value TOMPROD will be used instead");
            return zTG1.TELEGRAM_DELIMITER_HEADER_KTECH; // Default Übergabe bei Fehler
        }

    }
}
