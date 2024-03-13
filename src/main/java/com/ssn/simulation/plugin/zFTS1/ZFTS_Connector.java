package com.ssn.simulation.plugin.zFTS1;

import java.awt.Color;
import java.awt.Graphics;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.ssi.wasoc.WaSoc;
import com.ssi.wasoc.api.WaSocConnectionRegistry;
import com.ssi.wasoc.api.WaSocTelegramHandler;
import com.ssi.wasoc.api.executor.WaSocThreadPoolExecutorFactory;
import com.ssi.wasoc.api.telegram.InboundTelegram;
import com.ssi.wasoc.api.telegram.OutboundTelegram;
import com.ssn.simulation.core.Entity;
import com.ssn.simulation.editor.ValidationMessage;
import com.ssn.simulation.editor.Viewport;
import com.ssn.simulation.entities.AntSimSocConfig;
import com.ssn.simulation.entities.weasel.Weasel;
import com.ssn.simulation.entities.weasel.WeaselConnector;
import com.ssn.simulation.entities.weasel.WeaselSegementGroup;
import com.ssn.simulation.events.TelegramEvent;
import com.ssn.simulation.telegrams.weasel.WeaselConfigReply;
import com.ssn.simulation.telegrams.weasel.WeaselConfigRequest;
import com.ssn.simulation.telegrams.weasel.WeaselOrder;
import com.ssn.simulation.telegrams.weasel.WeaselStatus;
import com.ssn.simulation.telegrams.weasel.WeaselTelegram;

public class ZFTS_Connector extends Entity implements WaSocTelegramHandler {

    @AntSimSocConfig
    protected String controllerIp;

    @AntSimSocConfig
    protected int controllerPort;

    protected volatile transient LinkedBlockingQueue<zTG1> telegrams;
    protected volatile transient Map<String, Entity> handlers;
    protected volatile transient Map<Integer, zTG1> sequencecheck;
    protected volatile transient Map<Integer, zFTS1> serials;
    protected volatile transient Map<String, zFTS1> Fleets;
    protected volatile transient Map<Integer, Socket> connections;
    protected volatile transient Set<zFTS1> FTS_Controller;
    protected volatile transient boolean runtime;
    protected volatile transient boolean connecting;
    protected volatile transient long lastWaitingTime;
    protected volatile transient String fleetId;
    protected volatile transient zFTS1 ControllerFTS;

    public ByteHandler byteHandler;

    private WaSocThreadPoolExecutorFactory executorFactory;

    private WaSocConnectionRegistry connectionRegistry;

    private int sqn;

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
    }

    @Override
    public void onPropertiesChanged() {
        this.controllerIp = getStringProperty("controllerIp");
        this.controllerPort = getIntegerProperty("controlerPort");
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
            // Logik abprüfen ob ein FTFController vorhanden ist, welcher mehrere FTFs
            // kontrolliert, cn1, wenn ja dann genau diesen FTFController einbauen
            core.logError(this, "check 71 connector passed");
            if (!serials.isEmpty()) {
                // Den ersten Eintrag der Map ausgeben
                // Änderung durchgeführt , Herausziehen der Controller Entität,
                // sichern per FleetID Vergleich
                // Entscheidung diverse IFs obsolet
                if (telegram != null) {
                    core.logError(this, "check 73 connector passed");
                    TelegramEvent event = new TelegramEvent();
                    event.setTime(core.now());
                    event.setEntity(ControllerFTS);
                    event.setTelegram(telegram);
                    core.addEvent(event);
                } else {
                    core.logError(this, "handler not found for " + telegram);
                }

            }
        }
        // core.logError(this, "onNotify actually executed");
    };

    public void checkDistances() {
        // Logik für Distanzkontroller
    }

    public void checkSegmentGroups() {
        // Logik für Segment checken
    }

    @Override
    public void onReset() {
        super.onReset();
        this.telegrams = new LinkedBlockingQueue<>();
        this.handlers = new HashMap<>();
        this.serials = new HashMap<>();
        this.connections = new HashMap<>();
        this.FTS_Controller = new HashSet<>();
        this.connecting = false;
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
            telegram.setSender(zTG1.TELEGRAM_DELIMITER_HEADER2);
            telegram.setReceiver(zTG1.TELEGRAM_DELIMITER_HEADER1); // unsicher cn1
            telegram.setSequencenumber(sqn);
            if (sqn == 9999) {
                sqn = 1;
            } else {
                sqn += 1;
            }
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

    public void sendTelegramold(Entity sender, zTG1 telegram) { // Logik auf gespeicherten Byte Array und Controller
                                                                // Logik ergänzt
        try {
            if (sender instanceof zFTS1) {
                zFTS1 FTS = (zFTS1) sender;
                if (telegram instanceof zTG1) {
                    byte[] byteArray = null; // cn1 getByteOutputsream Logik fehlt;
                    if (telegram.confirmed == false) {
                        byteArray = telegram.confarray; // Logik für Quittierungstelegramm aus ursprünglichem String
                        telegram.confirmed = true;
                    } else {
                        return; // Logik um "normale" Telegramme in Richtung SAP zu schicken
                    }
                    Socket socket = connections.get(this.ControllerFTS);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(byteArray);
                    outputStream.flush();
                    core.logSend(telegram);
                }
            }
        } catch (Exception e) {
            core.logError(this, "unable to send ftf telegram " + telegram + ":" + e, e);
            zFTS1 FTS = (zFTS1) sender;
            Socket socket = connections.get(this.ControllerFTS);
            try {
                core.logInfo(this, "close socket for FTSController" + this.ControllerFTS);
                socket.getInputStream().close();
                socket.close();
            } catch (Exception e2) {
                core.logWarn(this, "unable to close socket for FTSController " + this.ControllerFTS);
            }
            core.logInfo(this, "try to re-connect");
            connect();
        }
    }

    public void receiveTelegrams() {
        synchronized (connections) {
            if (!connections.isEmpty()) {
                for (Entry<Integer, Socket> client : connections.entrySet()) {
                    Socket socket = client.getValue();
                    Integer serialNumber = client.getKey();
                    String encoding = "UTF-8";
                    if (socket.isConnected()) {
                        try {
                            InputStream in = socket.getInputStream();
                            int available = in.available();
                            if (available != 0) {
                                StringBuffer startcheck = new StringBuffer(); // neuer Stringbuffer
                                int i = 0; // neue Zählervariable // vermutlich fehlt initialisierung (evtl als
                                           // Instanzvariable einbinden besser)
                                InputStream bufin = in;
                                StringBuffer telegramValues = new StringBuffer();
                                int byteValue = 0;
                                String stringValue = "";
                                byte[] arr = new byte[1];
                                boolean startFound = false;
                                core.logError(this, "check 1 connector passed");
                                while (byteValue != -1 || stringValue.equals(zTG1.TELEGRAM_DELIMITER_START)) {
                                    core.logError(this, "check 2 connector passed");
                                    byteValue = bufin.read();
                                    arr[0] = (byte) byteValue;
                                    stringValue = new String(arr, encoding); // neu evtl. Grund für Fehler
                                    if (stringValue.equals(zTG1.TELEGRAM_DELIMITER_START) || i > 0) { // Neue Logik
                                                                                                      // Schritt 1
                                                                                                      // Abfrage auf "E"
                                        core.logError(this, "check 3 connector passed");
                                        startcheck.append(stringValue);
                                        i += 1;
                                        // in folgende IF Abfrage wird nicht navigiert
                                        if (startcheck.toString().equals(zTG1.TELEGRAM_DELIMITER_START_ALL)) { // Schritt
                                                                                                               // 2
                                                                                                               // Abfrage
                                            // gesamten Senders
                                            // nach 6
                                            // Durchgängen
                                            core.logError(this, "check 4 connector passed");
                                            telegramValues.append(startcheck); // hier vermutlich nicht, da
                                                                               // telegrammvalues vollkommen leer
                                            startFound = true;
                                            break;
                                        }
                                    }
                                }
                                if (startFound) { // Neue Logik für Endzeichenprüfung
                                    core.logError(this, "check 6 connector passed");
                                    String lastcheck = null;
                                    while (byteValue != -1
                                            || stringValue.equals(zTG1.TELEGRAM_DELIMITER_END_1)) {
                                        byteValue = bufin.read();
                                        arr[0] = (byte) byteValue;
                                        stringValue = new String(arr, encoding);
                                        if (stringValue.equals(zTG1.TELEGRAM_DELIMITER_END_1)
                                                && lastcheck.equals(zTG1.TELEGRAM_DELIMITER_END_1)) { // evtl_cn1_139
                                            core.logError(this, "check 7 connector passed"); // springt nicht hier rein
                                            telegramValues.append(stringValue);
                                            break;
                                        } else {
                                            telegramValues.append(stringValue);
                                            core.logError(this, "check 11 connector passed" + telegramValues + i);
                                            lastcheck = stringValue; // Speichern des letzten Bytes um später zu
                                                                     // vergleichen
                                            i += 1; // weiterzählen der Bytes
                                        }
                                    }
                                }
                                if (telegramValues.length() == 140) { // Änderung der Logik mitsamt Zwischenschaltung
                                                                      // der
                                                                      // Controller Entität
                                    // bisher numbercheck wie in Referenzconnector
                                    i = 0; // löst initialiserungsproblem vermutlich nicht cn1
                                    zTG1 telegram = null;
                                    core.logError(this, "Test 99 following");
                                    telegram = zTG1.interpret(telegramValues.toString()); // Telegramm mit
                                    // richtigen
                                    // Ausprägungen steht
                                    // zur
                                    // Verfügung

                                    if (telegram != null) { // cn1 Änderungen auf
                                                            // Controller Entität, Vorsicht, konkretes FTF nicht
                                                            // in Telegramm vorgegeben
                                        // Sequencenumber check -v cn1, es könnte über eine lokale Variable
                                        // gezählt werden, wenn der neue wert (sqn) größer ist alles gut,
                                        // ansonsten abfangen -> Entscheidung auf Controller wegen Erweiterbarkeit
                                        // if (!sequencecheck.containsKey(telegram.sequencenumber)) {
                                        // Entity zController = serials.get(serialNumber);

                                        core.logError(this, "check 8 connector passed");
                                        receiveTelegram(telegram);

                                        core.logError(this, "Telegramm bereits vorhanden");

                                    } else {
                                        core.logError(this, "unable to receive telegram, telegram creation failed: "
                                                + telegramValues.toString());
                                    }
                                } else {
                                    core.logError(this, "unable to receive telegram, received telegram is empty");
                                }
                            }
                        } catch (Exception e) {
                            core.logError(this, "unable to receive telegram, unexpected exception: " + e, e);
                        }
                    } else {
                        core.logError(this, "unable to receive telegram, socket is disconnected: " + client);
                    }
                }
            }
        }

    }

    public void receiveTelegram(zTG1 telegram) {
        core.logReceive(telegram);
        core.logInfo(this, "receives a FTF telegram: " + telegram.toString());
        try {
            telegrams.put(telegram);
            core.logError(this, "successfully added ");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

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

    public void connectold() {
        if (connecting) {
            return;
        }
        connecting = true;
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (connections) {
                    connections.clear();
                    try {
                        if (controllerIp != null) {
                            if (controllerIp.equals("localhost")) {
                                String ip = InetAddress.getLocalHost().getHostAddress();
                                controllerIp = ip;
                            } // eigene Logik, keine Schleife notwendig da nur
                              // 1 Controller
                            Socket clientSocket = new Socket(controllerIp, controllerPort);
                            if (!connections.containsKey(ControllerFTS.getFleetID())) {
                                connections.put(Integer.parseInt(ControllerFTS.getFleetID()), clientSocket);
                                core.logInfo(this, "weasel connection at controller ip " + controllerIp
                                        + " and controller port " + controllerPort + " created");
                            }
                        }
                    } catch (Exception e) {
                        core.logError(ZFTS_Connector.this, "unable to connect or re-connect: " + e, e);
                        onErrorOn();
                    }
                }
                connecting = false;
            }
        });
        thread.setDaemon(true);
        thread.setName(id + "-CONNECT");
        thread.start();

    }

    public void disconnect() {
        executorFactory.stopAll();
    }

    public LinkedBlockingQueue<zTG1> getTelegrams() {
        return telegrams;
    }

    @Override
    public boolean handleTelegram(InboundTelegram telegram) {
        try {// TODO Auto-generated method stub
            var header = this.byteHandler.read(telegram.getPayload(), zTG1.class);
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
                    this.receiveTelegram(poso);
                    break;
            }
            ;
        } catch (ByteReadException e) {

        }

        return true;

    }
}
