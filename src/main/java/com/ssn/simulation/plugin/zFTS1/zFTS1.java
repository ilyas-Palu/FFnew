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
import com.ssn.simulation.editor.IntegerListProperty;

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
    protected boolean ignoreHuId;
    protected int paarbitDuration_s;
    protected List<Integer> relevantWpcode_ProductionArea;
    protected int minWpcode_ProductionArea;
    protected int capacityCheck_s;
    protected String wartePlatz;
    protected boolean createHU;


    @JsonIgnore
    protected transient Map<String, zFTS_Entity1> FTSR; // FTS Klasse
    @JsonIgnore
    protected transient Map<Integer, zFTS_Waypoint> waypoints;
    @JsonIgnore
    protected transient Map<zFTS_Entity1, zTG1> FTFOrder;
    @JsonIgnore
    public transient Map<zTG1_WTSK, Entity> paarbitWtsk; // paarbit Map
    @JsonIgnore
    protected transient Map<zTG1, zFTS_Entity1> FTFOrderpast; // Archivierung vergangener Orders
    @JsonIgnore
    protected transient List<zTG1> orders; // eigene Telegrammklasse
    @JsonIgnore
    protected transient ZFTS_Connector connector; // ersetzen mit Telegrammspezifischen Verbinder
    @JsonIgnore
    protected transient Map<zTG1, Boolean> delayList; // Liste für zurückgestellte Telegramme

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
        this.ignoreHuId = false;
        this.senderId = 321;
        this.paarbitDuration_s = 300;
        this.minWpcode_ProductionArea = 51;
        relevantWpcode_ProductionArea = new ArrayList<>();
        this.capacityCheck_s = 3;
        this.wartePlatz = "430-R0334-WP-01";
        this.createHU = false;
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
        setBooleanProperty("ignoreHuId", ignoreHuId, "FTF Controller");
        setIntegerProperty("paarbitDuration", paarbitDuration_s, "FTF Controller");
        setListProperty("relevantWpcode_ProductionArea",
                relevantWpcode_ProductionArea, "FTF Controller", new IntegerListProperty());
        setIntegerProperty("minWpcode_ProductionArea", minWpcode_ProductionArea, "FTF Controller");
        setIntegerProperty("capacityCheck_s", capacityCheck_s, "FTF Controller");
        setStringProperty("wartePlatz", wartePlatz, "FTF Controller");
        setBooleanProperty("createHU", createHU, "FTF Controller");
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
        this.ignoreHuId = getBooleanProperty("ignoreHuId");
        this.paarbitDuration_s = getIntegerProperty("paarbitDuration");
        this.relevantWpcode_ProductionArea = getListProperty("relevantWpcode_ProductionArea", Integer.class);
        this.minWpcode_ProductionArea = getIntegerProperty("minWpcode_ProductionArea");
        this.capacityCheck_s = getIntegerProperty("capacityCheck_s");
        this.wartePlatz = getStringProperty("wartePlatz");
        this.createHU = getBooleanProperty("createHU");
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
        this.delayList = new LinkedHashMap<>();
        for (Entity entity : core.getEntities()) {
            if (entity instanceof zFTS_Entity1) {
                zFTS_Entity1 FTF = (zFTS_Entity1) entity;
                if (FTF.getFleetId().equals(fleetId)) {
                    this.FTSR.put(FTF.getId(), FTF);
                }
            }
            if (entity instanceof zFTS_Waypoint) {
                zFTS_Waypoint waypoint = (zFTS_Waypoint) entity;
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
        this.orders = new ArrayList<>();
        core.addNotifier(this);
    }

    @Override
    public void onNotify() { // angepasst
        Collection<zFTS_Entity1> all = FTSR.values();
        for (zFTS_Entity1 FTF : all) {
            try {
                FTF.onNotify();
            } catch (Exception e) {
                core.logError(this, " FTF mit ID " + FTF.getId() + "hat unbekannten Fehler: " + e
                        + " alle Aufträge des FTF werden abgebrochen ");
                FTF.DestinationWay1.clear();
                FTF.moveFTF(FTF.getHomewp());
            }
        }
    }

    @Override
    public void onStarted() {
        super.onStarted();

    }

    @Override
    public String getTelegramHandlerId() {
        return Integer.toString(senderId);
    }

    // Aufgerufen wenn ein Telegramm an Entität übergeben wird cn1 -> eigener
    // Connector wird Controller Telegramm weiterleiten (bzw. FTS nach oben)
    @Override
    public void onTelegram(Telegram telegram) {

        // hier Sequencechek bzw. Quittierungserkennung möglich cn1
        if (telegram instanceof zTG1_POSO) {
            zTG1_POSO TPoso = (zTG1_POSO) telegram;
            core.logInfo(this, "Poso Telegramm erhalten"); // Telegramm Logik
            useUnutiliziedFTFtg(TPoso, true);
            // triggerEntity();
        }

        if (telegram instanceof zTG1_WTSK) {

            zTG1_WTSK TWTSK = (zTG1_WTSK) telegram;

            core.logInfo(this, "WTsk Telegramm erhalten"); // Telegramm Logik

            handleWTSK(TWTSK);

        }

    }

    // wird normalerweise über input oder output Entity aufgerufen, hier zusätzlich
    // manuell
    @Override
    public void onTrigger(Entity entity) {
        super.onTrigger(entity);
        if (!FTFOrder.isEmpty()) {
            // Erster Schlüssel
            try {
                Entry<zFTS_Entity1, zTG1> firstEntry = this.FTFOrder.entrySet().iterator().next();
                zFTS_Entity1 firstKey = firstEntry.getKey(); // FTF
                zTG1 firstValue = firstEntry.getValue();
                System.out.println("Der erste Schlüssel ist: " + firstKey + ", sein Wert ist: " + firstValue);
                this.FTFOrder.remove(firstKey); // löschen des extrahierten Eintrges um nächsten zu nehmen
                if (firstValue instanceof zTG1_POSO) {
                    this.FTFOrderpast.put(firstValue, firstKey); // Archivierung
                    zTG1_POSO posoValue = (zTG1_POSO) firstValue;
                    if (firstKey != null) { // Standart Befehl FTF Bewegung aber konkret Förderer der Positionierung

                        core.logInfo(this, "assign Poso order " + " to FTF " + firstKey);
                        firstKey.setPosoOrder(posoValue); // vorheriger Downcast notwendig
                        posoValue.setFTFId(firstKey.getId());
                        posoValue.setAssigned(true);

                    }
                }
                if (firstValue instanceof zTG1_WTSK) {
                    this.FTFOrderpast.put(firstValue, firstKey); // Archivierung WTSK
                    zTG1_WTSK wtValue = (zTG1_WTSK) firstValue;
                    if (firstKey != null) {
                        core.logInfo(this, "assign WTSK order " + " to FTF " + firstKey);
                        firstKey.setWTSKOrder(wtValue);
                        wtValue.setFTFId(firstKey.getId());
                        wtValue.setAssigned(true);
                    }

                }
            } catch (NullPointerException e) {
                // Hier Behandlung für eine NullPointerException einfügen
                core.logError(this,
                        "NullPointerException beim Zugriff auf den ersten Eintrag der FTFOrder-Map: " + e.getMessage());
                FTFOrder.clear();
                return;
            } catch (NoSuchElementException e) {
                // Hier Behandlung für das Entfernen eines nicht vorhandenen
                // Elements einfügen
                core.logError(this,
                        "NoSuchElementException beim Entfernen eines Elements aus der FTFOrder-Map: " + e.getMessage());
                return;
            }
        }

    }

    // Standard senden an Connector, aufrufen aus FTF heraus

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
        } else {
            core.logError(this, "unable to send telegram, no known telegrammtype for: " + telegram);
        }
    }

    public boolean hasPrio(zTG1 TG1) {
        if (TG1 instanceof zTG1_WTSK) {
            zTG1_WTSK TWtsk = (zTG1_WTSK) TG1;
            if (TWtsk.Prioritätsbit.equals("X")) {
                return true;
            }
        }
        return false;

    }

    public void handleWTSK(zTG1_WTSK TWtsk) {

        // Paarbitabfrage vor POSO, weil Bezug auf POSO bei Paarbit WTsk nicht vorhanden
        if (TWtsk.Paarbit.equals("X") && paarbitDuration_s > 0) { // cn1 unsicher welches Zeichen Paarbit kennzeichnet

            this.handlePaarbit(TWtsk);
            return;

        }

        // Iteration über die Einträge der Map
        // Abfrage ob Quellplatz und HU_Nummer bereits vorhanden in Archiv
        for (Map.Entry<zTG1, zFTS_Entity1> entry : FTFOrderpast.entrySet()) {
            try {
                zFTS_Entity1 ftfkey = entry.getValue();
                zTG1 TGvalue = entry.getKey();

                // Überprüfen, ob die Werte übereinstimmen
                if (TGvalue instanceof zTG1_POSO) {
                    zTG1_POSO Tvalue = (zTG1_POSO) TGvalue;
                    if (Tvalue.assigned && Tvalue.HU_Nummer.equals(TWtsk.HU_Nummer)
                            && Tvalue.Quelle.equals(TWtsk.Quelle)) {
                        zFTS_Entity1 FTF = ftfkey;
                        core.logInfo(this, "passender POSO zu WTSK gefunden ");
                        FTFOrder.put(FTF, TWtsk); // FTFOrder befüllen für onTrigger Methode
                        onTrigger(this);
                        return;
                    }
                } else {
                    if (TGvalue instanceof zTG1_WTSK) {// Wartepositionslogik
                        zTG1_WTSK Tw1 = (zTG1_WTSK) TGvalue;
                        if (TWtsk.Quelle.equals(wartePlatz)) {
                            if (Tw1.Ziel.equals(TWtsk.Quelle) && Tw1.HU_Nummer.equals(TWtsk.HU_Nummer)) {
                                zFTS_Entity1 FTF = ftfkey;
                                core.logInfo(this, "passende Wartepositionslogik FTF gefunden");
                                FTFOrder.put(FTF, TWtsk);
                                onTrigger(this);
                                return;
                            }
                        }
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

        // FreeFTF Auslagerung
        this.useUnutiliziedFTFtg(TWtsk, true);

    }

    public void useUnutiliziedFTFtg(zTG1 TG1, boolean newTG) {

        zFTS_Entity1 newFTF = getFreeFTFInit(1); // Wenn vorher kein POSO gesendet wurde
        if (newFTF != null && delayList.isEmpty()) {

            FTFOrder.put(newFTF, TG1);
            onTrigger(this);
            return;
        }

        core.logInfo(this,
                "FTF Capacity is reached/nearly reached " + TG1 + " probably can not be assigned to a FTF for now"); // fehlende
        // Logging
        // Unterscheidung
        // cn1
        if (newTG) {
            this.addDelayEntry(TG1); // funktioniert nicht, weil vor Eventausführung Entry bereits gelöscht wurde,
                                     // und entsprechend wieder hinzugefügt wird change done
        }
        if (newFTF != null) {
            zTG1 nextTG = null;
            for (Map.Entry<zTG1, Boolean> entry : delayList.entrySet()) {
                if (entry.getValue() == true) {
                    nextTG = entry.getKey();
                    break;
                }
            }

            if (nextTG == null) {
                for (Map.Entry<zTG1, Boolean> entry : delayList.entrySet()) {
                    nextTG = entry.getKey();
                    break;
                }

            }
            FTFOrder.put(newFTF, nextTG);
            delayList.remove(nextTG);
            onTrigger(this);
            return;
        } else {
            // Event Erstellung
            zDelay dEvent = new zDelay(core.now() + (capacityCheck_s * 1000), this, TG1);
            core.addEvent(dEvent);
        }

    }

    public void addDelayEntry(zTG1 TG1) {

        if (!delayList.containsKey(TG1)) {
            Boolean prioCheck = this.hasPrio(TG1);
            delayList.put(TG1, prioCheck);
        }

    }

    public void handlePaarbit(zTG1_WTSK tWtsk) {
        Entity Paarbit = core.getEntityById(tWtsk.Quelle);
        paarbitWtsk.put(tWtsk, Paarbit); // Hinzufügen zur Liste
        zPaarbit pbEvent = new zPaarbit(core.now() + this.paarbitDuration_s * 1000, this, tWtsk); 
        core.addEvent(pbEvent);
        core.logInfo(this, "Paarbit Event added, WTSK will be seperately executed if no matching FTF/WTSK within "
                + paarbitDuration_s + " seconds");
    }

    public zFTS_Entity1 getFreeFTFInit(int waypoint) {

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

        return null;
    }

    public String getFleetID() {
        return this.fleetId;
    }

    public Map<zTG1, zFTS_Entity1> getOrderpast() {
        return this.FTFOrderpast;
    }

    public void inspectPaarbit(zTG1_WTSK paarbitTG) {

        for (Map.Entry<zTG1_WTSK, Entity> entry : paarbitWtsk.entrySet()) {
            if (entry.getKey() == paarbitTG) {
                // paarbitTG.Paarbit = null; // um IF Paarbit Abfrage zu umgehen
                paarbitWtsk.remove(entry.getKey());
                useUnutiliziedFTFtg(paarbitTG, true);
            }

        }
    }

    public int getMinWpcode_ProductionArea() {
        return minWpcode_ProductionArea;
    }

    public List<Integer> getRelevantWpcode_ProductionArea() {
        return relevantWpcode_ProductionArea;
    }

    public boolean isIgnoreHuId() {
        return ignoreHuId;
    }

    public String getWartePlatz() {
        return wartePlatz;
    }
    public boolean isCreateHU() {
        return createHU;
    }


}
