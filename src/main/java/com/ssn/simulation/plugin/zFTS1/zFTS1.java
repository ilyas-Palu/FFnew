/*
 * Copyright (c) 2013 SSI Schaefer Noell GmbH
 *
 * $Header: $
 *
 * Change History
 *   $Log: $
 */

package com.ssn.simulation.plugin.zFTS1;

import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ssn.simulation.core.Core;
import com.ssn.simulation.core.Entity;
import com.ssn.simulation.entities.ConnectorNGKP;
import com.ssn.simulation.entities.PalletController;
import com.ssn.simulation.properties.RuntimeState;
import com.ssn.simulation.telegrams.Telegram;
import com.ssn.simulation.telegrams.ngkp.NGKPTelegram;
import com.ssn.simulation.telegrams.ngkp.TT2310;
import com.ssn.simulation.utils.CoreUtils;

/**
 * Controller Entity for FTS zImplementation
 */

public class zFTS1 extends Entity {

    protected int senderId;
    protected int receiverId;
    protected int senderPort;
    protected int receiverPort;
    protected String fleetId;
    protected int FTFControllerID;

    protected transient Map<String, zFTS_Entity1> FTSR; // FTS Klasse
    protected transient Map<Integer, zFTS_Waypoint> waypoints;
    @RuntimeState
    protected transient Map<zFTS_Entity1, zTG1> FTFOrder;
    @RuntimeState
    public transient Map<Entity, zTG1_WTSK> paarbitWtsk; // paarbit Map
    @RuntimeState
    protected transient Map<zTG1, zFTS_Entity1> FTFOrderpast; // Archivierung vergangener Orders
    protected transient List<zTG1> orders; // eigene Telegrammklasse
    protected transient ZFTS_Connector connector; // ersetzen mit Telegrammspezifischen Verbinder

    @RuntimeState
    protected transient zTG1 order;

    // Konstruktor mit default Wertzuweisung, zusätzlich zu Entity Konstruktor
    public zFTS1() {
        super();
        this.sizex = 1;
        this.sizey = 1;
        this.sizez = 1;
        this.transparent = true;
        this.senderId = 0;
        this.receiverId = 201;
        this.senderPort = 0;
        this.receiverPort = 0;
        this.fleetId = "121212";
        this.senderId = 321;
    }

    // bei Bearbeitung der Attribute
    @Override
    public void onAttributesChanged() {
        super.onAttributesChanged();
        setIntegerProperty("senderId", senderId, 0, "FTF Controller");
        setIntegerProperty("receiverId", receiverId, 0, "FTF Controller");
        setIntegerProperty("senderPort", senderPort, 0, "FTF Controller");
        setIntegerProperty("receiverPort", receiverPort, 0, "FTF Controller");
        setStringProperty("fleetId", fleetId, "FTF Controller");
    }

    // bei Bearbeitung der Eigenschaften
    @Override
    public void onPropertiesChanged() {
        super.onPropertiesChanged();
        this.senderId = getIntegerProperty("senderId");
        this.receiverId = getIntegerProperty("receiverId");
        this.senderPort = getIntegerProperty("senderPort");
        this.receiverPort = getIntegerProperty("receiverPort");
        this.fleetId = getStringProperty("fleetId");
    }

    // Kategorie unter Menübaum

    @Override
    public String getCategory() {
        return "FTS";
    }

    // bei Starten des Modells im Runtime Modus
    // Erstellt 2 Listen und fügt nach Prüfung aller vorhandenen Entitäten
    // diejenigen der Liste hinzu die für sie relevant sind
    @Override
    public void onReset() {
        super.onReset();
        this.FTSR = new HashMap<>();
        this.waypoints = new HashMap<>();
        this.paarbitWtsk = new HashMap<>();
        this.FTFOrder = new LinkedHashMap<>(); // neue Map zur Verbindung der Orders mit FTF Entität -> Linked für
                                               // korrekte Reihenfolge
        this.FTFOrderpast = new LinkedHashMap<>(); // neue Map zur Archivierung
        for (Entity entity : core.getEntities()) {
            if (entity instanceof zFTS_Entity1) { // cn1 Ersetzen mit eigener Entität
                zFTS_Entity1 FTF = (zFTS_Entity1) entity;
                if (FTF.getFleetId().equals(fleetId)) {
                    this.FTSR.put(FTF.getId(), FTF);
                }
            }
            if (entity instanceof zFTS_Waypoint) {
                zFTS_Waypoint waypoint = (zFTS_Waypoint) entity; // cn1 Ersetzen mit eigener Entität
                if (waypoint.getWaypointCode() != 0) {
                    if (waypoint.getFleetId().equals(fleetId)) {
                        this.waypoints.put(waypoint.getWaypointCode(), waypoint);
                    }
                }
            }
            if (entity instanceof ZFTS_Connector) {
                connector = (ZFTS_Connector) entity;
            }
        }
        this.orders = new ArrayList<>(); // ersetzen mit eigener Telegrammliste der neuen Telegrammklassen
        // connector = CoreUtils.getConnectorNGKP(core, senderId, this); // ersetzen mit
        // zFTS Connector, evtl nicht
        // notwendig da keine unterschiedlichen Conveyor
        // Systeme
        // Somit basic core Entity Abfrage evtl
        // ausreichend cn1
        core.addNotifier(this);
    }

    @Override
    public void onNotify() {
        Collection<zFTS_Entity1> all = FTSR.values();
        for (zFTS_Entity1 FTF : all) {
            FTF.onNotify();

        }
    }

    @Override
    public void onStarted() {
        super.onStarted();
        handleStatusRequest(0);

    }

    @Override
    public String getTelegramHandlerId() {
        return Integer.toString(senderId);
    }

    // Beginn Callback Methods

    // Aufgerufen wenn ein Telegramm an Entität übergeben wird cn1 -> eigener
    // Connector wird Controller Telegramm weiterleiten (bzw. FTS nach oben)
    @Override
    public void onTelegram(Telegram telegram) {

        // hier Sequencechek bzw. Quittierungserkennung möglich cn1
        if (telegram instanceof zTG1_POSO) {
            zTG1_POSO TPoso = (zTG1_POSO) telegram;

            core.logInfo(this, "Poso Telegramm erhalten"); // Telegramm Logik
            handlePOSO(TPoso);
            triggerEntity();
        }

        if (telegram instanceof zTG1_WTSK) {

            zTG1_WTSK TWTSK = (zTG1_WTSK) telegram;

            core.logInfo(this, "WTsk Telegramm erhalten"); // Telegramm Logik

            handleWTSK(TWTSK);

            // erstmal nur Logik inklusive vorherigem POSO Telegramm
            // Zielförderer Koordinaten

            // Logik Fahrverarbeitung
        }

        if (telegram instanceof zTG1_LIFE) {
            core.logInfo(this, "Life Telegramm erhalten"); // Life Telegramm Logik
            zTG1_LIFE TLife = (zTG1_LIFE) telegram;
            sendTelegram(TLife, this);
        }
        // Logik für Telegrammverarbeitung Richtung SAP

        // connector.sendTelegram(rt1); //vermutlich cn1

    }

    {
        // Logik für Telegrammverarbeitung Richtung FTS
    }

    // wird normalerweise über input oder output Entity aufgerufen
    @Override
    public void onTrigger(Entity entity) {
        super.onTrigger(entity);
        if (!FTFOrder.isEmpty()) {// For Schleife einfügen (Vermutlich für mehrere Telegramme auf einmal)
            // Erster Schlüssel
            Entry<zFTS_Entity1, zTG1> firstEntry = this.FTFOrder.entrySet().iterator().next();
            zFTS_Entity1 firstKey = firstEntry.getKey(); // FTF
            zTG1 firstValue = firstEntry.getValue(); // Telegramm, um allgemeine Logik/Subtypunabhängig erweitern,
                                                     // Instanzabfrage erst nach nächstem IF, Reihenfolge beachten
            System.out.println("Der erste Schlüssel ist: " + firstKey + ", sein Wert ist: " + firstValue);
            //
            this.FTFOrder.remove(firstKey); // löschen des extrahierten Eintrges um nächsten zu nehmen
            if (firstValue instanceof zTG1_POSO) {
                this.FTFOrderpast.put(firstValue, firstKey); // Archivierung
                zTG1_POSO posoValue = (zTG1_POSO) firstValue;
                if (firstKey != null) { // Standart Befehl FTF Bewegung aber konkret Förderer der Positionierung
                    // Vorsicht destinationpoints können sich bei unserem TG nicht aus Inhalt
                    // gezogen werden
                    core.logError(this, "assign Poso order " + " to FTF " + firstKey);
                    firstKey.setPosoOrder(posoValue); // vorheriger Downcast notwendig
                    posoValue.setFTFId(firstKey.getId());
                    posoValue.setAssigned(true);

                    //
                    // firstKey.handleStartNotification();
                }
            }
            if (firstValue instanceof zTG1_WTSK) {
                zTG1_WTSK wtValue = (zTG1_WTSK) firstValue;
                if (firstKey != null) {
                    core.logError(this, "assign WTSK order " + " to FTF " + firstKey);
                    firstKey.setWTSKOrder(wtValue);
                    wtValue.setFTFId(firstKey.getId());
                    wtValue.setAssigned(true);
                }

            }
        }

    }

    // ENDE Callback Methods
    // ---------------------------

    // Beginn zFunktionen und Telegrammverarbeitung

    public void handleStatusRequest(int requestId) {

    }

    public void sendTelegram(zTG1 telegram, Entity sender) {
        if (connector != null) {
            sender = this;
            core.logError(this, "909 Telegramm hat Controller erreicht ( " + telegram.telegramsubtype);
            if (telegram.telegramsubtype == zTG1_INFO.TELEGRAM_TYPE) {
                connector.sendTelegram(telegram);
                core.logError(this, "909 Telegramm hat Controller erreicht ( " + telegram.telegramsubtype);
            }
            if (telegram.telegramsubtype == zTG1_WTCO.TELEGRAM_TYPE) {
                connector.sendTelegram(telegram);
                core.logError(this, "909 Telegramm hat Controller erreicht ( " + telegram.telegramsubtype);
            }
            // cn1 Konver
            // connector.sendTelegram(sender, telegram); // sende Funktion Connector
            // eingebaut
        } else {
            core.logError(this, "unable to send telegram, no known telegrammtype: " + telegram);
        }
    }

    public void handlePOSO(zTG1_POSO TPoso) {
        String dest = TPoso.Quelle;
        try {
            Entity next = core.getEntityById(dest);
            zFTS_Entity1 useFTF = getFreeFTFInit(1); // Homeposition/Bahnhof
            core.logError(this, " 26 Ausgeführt werden soll POSO mit FTF " + useFTF);
            if (useFTF != null) {
                FTFOrder.put(useFTF, TPoso); // FTFOrder befüllen für onTrigger Methode
                // Methode Befehl zum Förderer bewegen
            } else {
                core.logError(this, " kein freies FTF gefunden ");
            }
        } catch (Exception e) {
            core.logError(TPoso, "Zielförderer nicht gefunden");
            return;
        }
        onTrigger(this);
        ; // Dictionary einbauen mit passendem FTF

    }

    public void handleWTSK(zTG1_WTSK TWtsk) {

        // Iteration über die Einträge der Map
        // Abfrage ob Quellplatz und HU_Nummer bereits vorhanden in Archiv
        for (Map.Entry<zTG1, zFTS_Entity1> entry : FTFOrderpast.entrySet()) {
            zFTS_Entity1 ftfkey = entry.getValue();
            zTG1 TGvalue = entry.getKey();
            if (TWtsk.Paarbit.equals("X")) { // cn1 unsicher welches Zeichen Paarbit kennzeichnet

                this.handlePaarbit(TWtsk);
                return;

            }
            // Überprüfen, ob die Werte übereinstimmen
            if (TGvalue instanceof zTG1_POSO) {
                core.logError(this, " 661 POSO gefunden !");
                zTG1_POSO Tvalue = (zTG1_POSO) TGvalue;
                core.logError(this, "checks 88 " + Tvalue.HU_Nummer + " (HU) " + TWtsk.HU_Nummer + "  " + Tvalue.Quelle
                        + " (Quelle) " + TWtsk.Quelle + " check b " + Tvalue.assigned);
                if (Tvalue.assigned && Tvalue.HU_Nummer.equals(TWtsk.HU_Nummer) && Tvalue.Quelle.equals(TWtsk.Quelle)) {
                    zFTS_Entity1 FTF = ftfkey;
                    core.logError(this, " 8182 " + FTF.getLastWaypointCode());
                    core.logError(this, " 8183 " + FTF.hasItem());
                    core.logError(this, "alle Prüfungen okay 24 " + FTF + " " + TWtsk);
                    FTFOrder.put(FTF, TWtsk); // FTFOrder befüllen für onTrigger Methode
                    // Logik assign ftf zu Telegram und Abfrage Koordinaten destination
                    onTrigger(this);
                    return; // weiterführende Logik notwendig cn1

                }
            }

        }
        zFTS_Entity1 newFTF = getFreeFTFInit(1); // Wenn vorher kein POSO gesendet wurde
        FTFOrder.put(newFTF, TWtsk);
        onTrigger(this);

    }

    // Kapazitäts und Zuordnungstests auf FTS bezogene Entitäten :

    public void handlePaarbit(zTG1_WTSK tWtsk) {
        Entity Paarbit = core.getEntityById(tWtsk.Quelle);
        paarbitWtsk.put(Paarbit, tWtsk);
        checkPaarbit(tWtsk);
    }

    public void checkPaarbit(zTG1_WTSK wtsk) {
        zPaarbit pbEvent = new zPaarbit(core.now() + 300000, this, wtsk);
    }

    public zFTS_Entity1 getFreeFTFInit(int waypoint) { // Anpassen auf initialen Bahnhof und Methodenergänzung cn1
        // jetzt durch Weasel Liste iterieren und erstes FTF wo Home Position =
        // current ist, verwenden (+not moving etc.), dann FTF destination zum
        // Auslagerstich übergeben

        if (waypoint != 0) {
            Collection<zFTS_Entity1> all = FTSR.values();
            for (zFTS_Entity1 FTF : all) {
                if (FTF.getPoso() == null) {
                    if (FTF.getLastWaypointCode() == waypoint) {
                        if (FTF.getNextWaypointCode() == 0) {
                            if (!FTF.hasDestinatiion()) {
                                return FTF;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getFleetID() {
        return this.fleetId;
    }

    public Map<zTG1, zFTS_Entity1> getOrderpast() {
        return this.FTFOrderpast;
    }

    public void inspectPaarbit(zTG1_WTSK paarbitTG) {

        for (Map.Entry<Entity, zTG1_WTSK> entry : paarbitWtsk.entrySet()) {
            if (entry.getValue() == paarbitTG) {
                paarbitTG.Paarbit = null;
                handleWTSK(paarbitTG);
                return;
            }

        }
    }
}
