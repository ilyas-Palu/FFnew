/*
 * Copyright (c) 2013 SSI Schaefer Noell GmbH
 *
 * $Header: $
 *
 * Change History
 *   $Log: $
 */

package com.ssn.simulation.plugin.zFTS1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.telegrams.Telegram;

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
    @JsonIgnore
    protected transient Map<String, zFTS_Entity1> FTSR; // FTS Klasse
    @JsonIgnore
    protected transient Map<Integer, zFTS_Waypoint> waypoints;
    @JsonIgnore
    protected transient Map<zFTS_Entity1, zTG1> FTFOrder;
    @JsonIgnore
    public transient Map<Entity, zTG1_WTSK> paarbitWtsk; // paarbit Map
    @JsonIgnore
    protected transient Map<zTG1, zFTS_Entity1> FTFOrderpast; // Archivierung vergangener Orders
    @JsonIgnore
    protected transient List<zTG1> orders; // eigene Telegrammklasse
    @JsonIgnore
    protected transient ZFTS_Connector connector; // ersetzen mit Telegrammspezifischen Verbinder

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
    public void onNotify() { // angepasst
        Collection<zFTS_Entity1> all = FTSR.values();
        for (zFTS_Entity1 FTF : all) {
            // if (!FTF.isAt(1) || FTF.poso != null || FTF.wtorder != null) {
            try {
                FTF.onNotify();
            } catch (Exception e) {
                core.logError(this, " FTF mit ID " + FTF.getId() + "hat unbekannten Fehler: " + e
                        + " alle Aufträge des FTF werden abgebrochen ");
                FTF.DestinationWay1.clear();
                FTF.moveWeasel(FTF.getHomewp());
            }
        }
        // }
    }

    @Override
    public void onStarted() {
        super.onStarted();

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
        // Logik für Telegrammverarbeitung Richtung SAP

        // connector.sendTelegram(rt1); //vermutlich cn1

    }

    // wird normalerweise über input oder output Entity aufgerufen
    @Override
    public void onTrigger(Entity entity) {
        super.onTrigger(entity);
        if (!FTFOrder.isEmpty()) {// For Schleife einfügen (Vermutlich für mehrere Telegramme auf einmal)
            // Erster Schlüssel
            try {
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
                        core.logInfo(this, "assign Poso order " + " to FTF " + firstKey);
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
                        core.logInfo(this, "assign WTSK order " + " to FTF " + firstKey);
                        firstKey.setWTSKOrder(wtValue);
                        wtValue.setFTFId(firstKey.getId());
                        wtValue.setAssigned(true);
                    }

                }
            } catch (NullPointerException e) {
                // Hier können Sie die Behandlung für eine NullPointerException einfügen
                core.logError(this,
                        "NullPointerException beim Zugriff auf den ersten Eintrag der FTFOrder-Map: " + e.getMessage());
                FTFOrder.clear();
                return;
            } catch (NoSuchElementException e) {
                // Hier können Sie die Behandlung für das Entfernen eines nicht vorhandenen
                // Elements einfügen
                core.logError(this,
                        "NoSuchElementException beim Entfernen eines Elements aus der FTFOrder-Map: " + e.getMessage());
                return;
            }
        }

    }

    // ENDE Callback Methods
    // ---------------------------

    // Beginn zFunktionen und Telegrammverarbeitung

    public void sendTelegram(zTG1 telegram, Entity sender) {
        if (connector != null && telegram != null) {
            sender = this;
            if (telegram.telegramsubtype == zTG1_INFO.TELEGRAM_TYPE) {
                connector.sendTelegram(telegram);
                core.logInfo(this, "Telegramm hat Controller erreicht Typ: " + telegram.telegramsubtype);
            }
            if (telegram.telegramsubtype == zTG1_WTCO.TELEGRAM_TYPE) {
                connector.sendTelegram(telegram);
                core.logInfo(this, "Telegramm hat Controller erreicht Typ: " + telegram.telegramsubtype);
            }
            // cn1 Konver
            // connector.sendTelegram(sender, telegram); // sende Funktion Connector
            // eingebaut
        } else {
            core.logError(this, "unable to send telegram, no known telegrammtype: " + telegram);
        }
    }

    public void handlePOSO(zTG1_POSO TPoso) {
        try {
            zFTS_Entity1 useFTF = getFreeFTFInit(1); // Homeposition/Bahnhof
            if (useFTF != null) {
                core.logInfo(this, " Ausgeführt werden soll POSO mit FTF " + useFTF);
                FTFOrder.put(useFTF, TPoso); // FTFOrder befüllen für onTrigger Methode
            } else {
                core.logError(this, " kein freies FTF gefunden ");
            }
        } catch (Exception e) {
            core.logError(TPoso, "FTF Zuweisung nicht möglich");
            return;
        }
        onTrigger(this);
        ; // Dictionary einbauen mit passendem FTF

    }

    public void handleWTSK(zTG1_WTSK TWtsk) {

        // Iteration über die Einträge der Map
        // Abfrage ob Quellplatz und HU_Nummer bereits vorhanden in Archiv
        for (Map.Entry<zTG1, zFTS_Entity1> entry : FTFOrderpast.entrySet()) {
            try {
                zFTS_Entity1 ftfkey = entry.getValue();
                zTG1 TGvalue = entry.getKey();

                if (TWtsk.Paarbit.equals("X")) { // cn1 unsicher welches Zeichen Paarbit kennzeichnet

                    // this.handlePaarbit(TWtsk);
                    // return;

                }
                // Überprüfen, ob die Werte übereinstimmen
                if (TGvalue instanceof zTG1_POSO) {
                    zTG1_POSO Tvalue = (zTG1_POSO) TGvalue;
                    if (Tvalue.assigned && Tvalue.HU_Nummer.equals(TWtsk.HU_Nummer)
                            && Tvalue.Quelle.equals(TWtsk.Quelle)) {
                        zFTS_Entity1 FTF = ftfkey;
                        core.logError(this, "passender POSO zu WTSK gefunden ");
                        FTFOrder.put(FTF, TWtsk); // FTFOrder befüllen für onTrigger Methode
                        // Logik assign ftf zu Telegram und Abfrage Koordinaten destination
                        onTrigger(this);
                        return; // weiterführende Logik notwendig cn

                    }
                }

            } catch (NullPointerException e) {
                core.logError(this, "Eine NullPointerException ist aufgetreten: " + e.getMessage());
                return;
            } catch (ClassCastException e) {
                core.logError(this, "Eine ClassCastException ist aufgetreten: " + e.getMessage());
                return;
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
                        if (!FTF.hasDestination()) {
                            return FTF;
                        }
                    }
                }

            }
        }

        core.logError(this, "notwendigen Verfügbarkeits Überprüfungen verhinden FTF Zuweisung");
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
