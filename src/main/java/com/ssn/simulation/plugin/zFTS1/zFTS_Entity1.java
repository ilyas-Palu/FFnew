package com.ssn.simulation.plugin.zFTS1;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.core.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class zFTS_Entity1 extends Entity {

    // real Entity of FTS (about 20 needed in Model)

    protected String fleetId;
    protected int startPosition; // Waypoint where FTS starts at runtime
    protected int homePosition;
    protected double travelSpeed;

    @JsonIgnore
    protected transient zFTS1 controller; // Ersetzen durch eigenen Controller
    @JsonIgnore
    protected transient int lastWaypointCode; // used ip
    @JsonIgnore
    protected transient Boolean paarbitActive;
    @JsonIgnore
    protected transient Entity destMach; // used
    @JsonIgnore
    protected transient Entity srcdest; // used
    @JsonIgnore
    protected transient Entity posoSrc; // used
    @JsonIgnore
    protected transient Set<zFTS_Waypoint> allWaypoints; // used
    @JsonIgnore
    protected transient zFTS_Waypoint from; // use like lastwaypointcode
    @JsonIgnore
    protected transient zFTS_Waypoint to; // currently used as home waypoint
    @JsonIgnore
    protected transient Map<String, zFTS_Waypoint> DestinationWay1; // Alle Destinations per Code
    @JsonIgnore
    protected transient zTG1_POSO poso;
    @JsonIgnore
    protected transient zTG1_WTSK wtorder;
    @JsonIgnore
    protected transient long posoTime;
    @JsonIgnore
    protected transient boolean assigned = false;
    @JsonIgnore
    protected transient boolean blockedTransfer = false;
    @JsonIgnore
    protected transient boolean waitingActive = false;

    public zFTS_Entity1() {
        sizex = 0.9;
        sizey = 0.7;
        fleetId = "121212";
        startPosition = 1;
        homePosition = 1;
        travelSpeed = 3.0;
    }

    // Kategorie unter Menübaum

    @Override
    public String getCategory() {
        return "FTS";
    }

    @Override
    public void onAttributesChanged() {
        super.onAttributesChanged();
        setStringProperty("fleetId", fleetId, "FTS");
        setIntegerProperty("startPosition", startPosition, "FTS");
        setIntegerProperty("homePosition", homePosition, "FTS");
        setDoubleProperty("travelSpeed", travelSpeed, 0.01, "FTS");

    }

    @Override
    public void onPropertiesChanged() {
        super.onPropertiesChanged();
        fleetId = getStringProperty("fleetId");
        startPosition = getIntegerProperty("startPosition");
        homePosition = getIntegerProperty("homePosition");
        travelSpeed = getDoubleProperty("travelSpeed");
    }

    @Override
    public Object clone() {
        try {
            zFTS_Entity1 entity = (zFTS_Entity1) super.clone();
            return entity;
        } catch (Exception e) {
            core.logError(this, "unable to clone entity: " + e.toString(), e);
            return null;
        }
    }

    @Override
    public void onReset() {
        super.onReset();
        controller = null;
        lastWaypointCode = 0;
        from = null;
        to = null;
        allWaypoints = new HashSet<>();
        destMach = null;
        poso = null;
        posoTime = 0;
        wtorder = null;
        posoSrc = null;
        paarbitActive = false;
        waitingActive = false;
        DestinationWay1 = new LinkedHashMap<>(); // neue Map zur Archivierung
        for (Entity entity : core.getEntities()) {
            if (entity instanceof zFTS1) { // Austausch eigene Controller Entität! cn1
                zFTS1 ctrl = (zFTS1) entity;
                if (ctrl.getFleetID().equals(fleetId)) {
                    controller = ctrl;
                }
            }
            if (startPosition != 0) {
                if (entity instanceof zFTS_Waypoint) {
                    zFTS_Waypoint waypoint = (zFTS_Waypoint) entity;
                    allWaypoints.add(waypoint); // Speichern aller Waypoints
                    if (waypoint.getWaypointCode() == startPosition) { // Zuweisen über startposition Code
                        if (waypoint.getFleetId().equals(fleetId)) {
                            to = waypoint; // Auffüllen to
                            from = to;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStarted() {
        super.onStarted();
        if (to != null) {
            setLastWaypointCode(to.getWaypointCode());
            setLayer(to.getLayer());
            setMoveStopx(to.getInterpolatedPosx());
            setMoveStopy(to.getInterpolatedPosy());
            setMoveStopz(to.getInterpolatedPosz());
            lastWaypointCode = to.getWaypointCode(); // übergeben homeposition aktuelle position
        }
    }

    @Override
    public void onNotify() {
        posx = getMoveStopx();
        posy = getMoveStopy();
        posz = getMoveStopz();
        if (to == null) {
            return;
        }
        if (!isError()) {
            if (!isMoving()) {
                if (!blockedTransfer) {
                    if (!waitingActive) {
                        if (!DestinationWay1.isEmpty()) {
                            Entry<String, zFTS_Waypoint> entry = DestinationWay1.entrySet().iterator().next();
                            zFTS_Waypoint zwp = entry.getValue();
                            String vProcess = entry.getKey();
                            // Abfrage ob Waypointcodes passen und diese noch nicht erreicht
                            if (vProcess.equals("POSO")) { // besser wieder ändern auf poso.subtype bzw. statisches
                                                           // Attribut
                                this.posoTime = core.now();
                                if ((zwp.getWaypointCode() == 11 || zwp.getWaypointCode() == 12
                                        || zwp.getWaypointCode() == 13) && this.isAt(to)) { // geändete von 1 auf to
                                    core.logInfo(this, "POSO wurde erkannt");
                                    this.moveFTF(zwp);
                                    lastWaypointCode = zwp.getWaypointCode(); // Übergabe aktuelle Position
                                    from = zwp; // Teiweise Rendundanz zwischen Lastwaypointcode & from
                                    return;
                                }

                                if (!this.posoSrc.hasItem()) {
                                    if (!this.posoSrc.hasItem()) {
                                        zInfo_MPOE checkEvent = new zInfo_MPOE(
                                                core.now() + (long) zwp.getMpoe_duration_ms());
                                        checkEvent.setConveyor(posoSrc);
                                        checkEvent.setWP(zwp.getWaypointCode());
                                        checkEvent.setFTF(this);
                                        core.addEvent(checkEvent);
                                    }
                                }

                                this.DestinationWay1.remove(vProcess);

                                return;

                            }

                            if (this.wtorder != null) {
                                if (vProcess.contains("WTSK")) {
                                    if (from == zwp) { // lastdest erreicht (bzw. Quellplatz)
                                        if (vProcess.contains("Q")) {
                                            if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {// ->im
                                                // Produktionsnetz
                                                this.moveproddestx(vProcess, false);
                                                if (!isMoving()) {
                                                    this.moveproddesty(vProcess, false);
                                                } else {
                                                    return;
                                                }
                                            }
                                            // Item Transfer cn1
                                            if (isMoving()) {
                                                return;
                                            } else if (!hasItem()) {
                                                handleSrcTransfer(core.now() + 5000, vProcess);
                                                if (blockedTransfer) {
                                                    return;
                                                }
                                            }

                                        } else {
                                            if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                                                this.moveproddestx(vProcess, false);
                                                if (!isMoving()) {
                                                    this.moveproddesty(vProcess, false);
                                                } else {
                                                    return;
                                                }

                                            }
                                            if (!isMoving()) {
                                                handleDestTransfer(core.now() + 30000);
                                                if (waitingActive) {
                                                    return;
                                                }
                                            } else {
                                                return;
                                            }
                                            // cn1 hier evtl Paarbit Abfrage in Verbindung mit WTCO TG (Paarbit Boolean
                                            // löschen)
                                            if (!paarbitActive) {
                                                this.wtorder = null;
                                                poso = null;
                                            }
                                        }
                                        this.DestinationWay1.remove(vProcess);

                                        // löschen Eintrag
                                        return;
                                    } else {
                                        if (this.isAt(from)) { // ->Routing möglich
                                            if (from.nextWaypoint(zwp.getWaypointCode()) != null) {
                                                from = from.nextWaypoint(zwp.getWaypointCode());
                                                core.logInfo(this,
                                                        "Waypoint Routing erfolgreich Beauftragung nach "
                                                                + from.getId());
                                                this.moveFTF(from);
                                                lastWaypointCode = from.getWaypointCode();
                                                return;
                                            }
                                        } else {
                                            if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                                                if (!isMoving()) {
                                                    moveFTF(from);
                                                    return;
                                                }
                                            } // cn1
                                        }
                                    }
                                    ;

                                }

                                // }
                            }

                        } else {

                            if (this.assigned == true && poso == null) { // um für WTSK in Controller Entität nicht zu
                                                                         // blocken
                                this.assigned = false; // evtl nach wtco 1mal ausführen
                            }

                            if (!this.isAt(to) && !this.hasItem() && (lastWaypointCode < 11 || lastWaypointCode > 14)) {
                                if (this.isAt(from)) {
                                    if (from.nextWaypoint(to.getWaypointCode()) != null) { // rendundante logik mit wtsk
                                                                                           // evtl
                                                                                           // auslagern
                                        from = from.nextWaypoint(to.getWaypointCode());
                                        core.logInfo(this,
                                                "Waypoint Routing erfolgreich, Beauftragung nach" + from.getId());
                                        this.moveFTF(from);
                                        lastWaypointCode = from.getWaypointCode();
                                        return;
                                    }
                                } else {
                                    this.moveOutMach();
                                    if (!this.isMoving()) {

                                        moveFTF(from);
                                    }
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMoved(double x, double y, double z) {
        super.onMoved(this.getPosx(), this.getPosy(), this.getPosz());
        setMoving(false);

    }

    public void moveproddestx(String ent, boolean rev) {
        float prx = 0;
        // float pry = 0;
        if ((ent.contains("Z") && !rev) || ((!ent.contains("Z")) && rev)) {
            prx = (float) this.destMach.getPosx();
            // pry = (float) this.destMach.getPosy(); // y eig irrelevant
        } else {
            prx = (float) this.srcdest.getPosx();
            // pry = (float) this.srcdest.getPosy();
        }
        if (rev) { // mittlerweile irrelevant
            prx = (float) this.from.getPosx();
        }
        if (this.posx - prx < 0.03 && this.posx - prx > -0.03) { // Abfrage ob Position bereits stimmt
            return;
        }

        // setMoving(true);
        this.moveWithSpeed(prx, from.getPosy(), destMach.getPosz(), 0);

    }

    public void moveproddesty(String ent, boolean rev) {// Abändern auf y Fahrt Logik (WP unabhängig!)
        float prx = 0;
        float pry = 0;
        float buf1 = 0;
        if ((ent.contains("Z") && !rev) || ((!ent.contains("Z")) && rev)) {
            prx = (float) this.destMach.getPosx();
            pry = (float) this.destMach.getPosy();
            buf1 = (float) ((destMach.getSizey()));
        } else {
            prx = (float) this.srcdest.getPosx();
            pry = (float) this.srcdest.getPosy();
            buf1 = (float) ((srcdest.getSizey()));
        }

        if (rev) {
            pry = (float) this.from.getPosy(); // mittlerweile irrelevant
        }

        if (pry > this.getPosy()) { // Anpassung mitsamt eigener Größe für Berechnung
            buf1 = pry - buf1;
        } else {
            buf1 = pry + buf1;
        }

        if (this.posy - buf1 < 0.03 && posy - buf1 > -0.03 || rev) { // Abfrage ob Position bereits stimmt
            return;
        }
        this.moveWithSpeed(prx, buf1, from.getPosz(), 0);

    }

    public void moveWithSpeed(double x1, double y1, double z1, long t1) {

        double realDistanceX = Math.abs(this.posx - x1); // Ergebniss immer positiv (Betrag)
        double realDistanceY = Math.abs(this.posy - y1); // Ergebniss immer positiv (Betrag)
        // bisher keine Z Betrachtung
        double totalDistance = realDistanceX + realDistanceY;

        long timeNeeded = ((long) (totalDistance / travelSpeed)) * 1000; // in s

        setMoving(true);

        moveEntity(x1, y1, z1, timeNeeded);

    }

    public void moveFTF(zFTS_Waypoint waypoint) {

        this.moveWithSpeed(waypoint.getPosx(), waypoint.getPosy(), waypoint.getPosz(), 0);

    }

    public boolean isAt(zFTS_Waypoint waypoint) {
        double tolerance = 0.05; // toleranz erhöht
        if (waypoint != null) {
            if (Math.abs(posx - waypoint.getInterpolatedPosx()) < tolerance) {
                if (Math.abs(posy - waypoint.getInterpolatedPosy()) < tolerance) {
                    if (Math.abs(posz - waypoint.getInterpolatedPosz()) < tolerance) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public boolean isAt(int code) {
        return lastWaypointCode == code;
    }

    public boolean hasOrder() {
        return poso != null;
    }

    public void moveOutMach() {
        // Methode für das standartisierte Herausfahren von Produktionsmaschine auf
        // WaypointStrecke

        if (this.posy == from.getPosy()) {
            return;
        }
        ;

        moveWithSpeed(this.posx, from.getPosy(), from.getPosz(), 0);

    }

    public void setPosoOrder(zTG1_POSO Posorder) {
        this.poso = Posorder; // Telegramm Positionierung
        this.posoSrc = core.getEntityById(poso.Quelle);
        zFTS_Waypoint wp1 = allmap(poso.Quelle);
        core.logInfo(this, "Mapping für POSO durchgeführt, Beauftragung nach " + wp1.getId());
        DestinationWay1.put(this.poso.telegramsubtype, wp1); // benötigten Code und Prozess
                                                             // abspeichern
    }

    public void setWTSKOrder(zTG1_WTSK WTOrder) {
        this.wtorder = WTOrder;
        String zwst = WTOrder.Ziel.replaceAll("\\.+$", "");
        String zwqq = WTOrder.Quelle.replaceAll("\\.+$", "");
        this.destMach = core.getEntityById(zwst); // konkrete Zielentität
        this.srcdest = core.getEntityById(zwqq); // konkrete Quellentität
        zFTS_Waypoint lastdest1 = allmap(zwst);
        zFTS_Waypoint firstsrc = allmap(zwqq);
        String combinedKeyQ = "WTSK-Q" + WTOrder.sequencenumber;
        String combinedKeyZ = "WTSK-Z" + WTOrder.sequencenumber;
        core.logInfo(this, "Beauftragung wtsk auf Entität " + lastdest1 + " wegen Ziel " + destMach);
        if (firstsrc != null) {
            DestinationWay1.put(combinedKeyQ, firstsrc); // eigentliche Quelle zuerst
        } else {
            // Fehlerbehandlung
            return;
        }
        DestinationWay1.put(combinedKeyZ, lastdest1); // eigentliches Ziel

        if (this.waitingActive == true) {
            this.waitingActive = false;
        }

    }

    public zFTS_Waypoint allmap(String dest) {
        for (zFTS_Waypoint element : allWaypoints) {
            if (element.getMatchEntity() != null) {
                String match = element.getMatchEntity();
                if (match.equals(dest)) {
                    return element;
                }
            }
        }
        return this.calcDest(core.getEntityById(dest));

    }

    public zFTS_Waypoint calcDest(Entity zDest) { // Berechne kürzeste Distanz //CN1 Achtung es sollen eig nicht alle
                                                  // zWaypoints betrachtet werden (nur Produktionsnetz) evtl Einbau
                                                  // Fleetid, nur mittlere PN WP checken!

        zFTS_Waypoint mindist = null;
        float dist1 = 99999; // Sauberer: positive unendlichkeit einbauen
        float comp1 = 0;

        for (zFTS_Waypoint element : allWaypoints) {

            // Durch Waypoints iterieren Koordinaten speichern und nähsten Zurückgeben
            comp1 = (float) zDest.distanceTo(element);
            // if (element.getWaypointCode() == 61 || element.getWaypointCode() == 64 ||
            // lement.getWaypointCode() == 67
            // || element.getWaypointCode() == 70 || element.getWaypointCode() == 73) {

            if (this.controller.getRelevantWpcode_ProductionArea().contains(element.getWaypointCode())) {
                if (comp1 < dist1) {
                    dist1 = comp1;
                    mindist = element;
                }
            }
        }

        return mindist;

    }

    public boolean hasDestination() {
        return !this.DestinationWay1.isEmpty();
    }

    public void setAssigned(boolean b) {
        this.assigned = b;
    }

    public void handleSrcTransfer(long timeEnd, String process) { // Allgemeine HU Transfer Methode auf FTF
        blockedTransfer = true;
        if (this.srcdest != null) {
            core.logInfo(this, " jetzt Durchführung der HU Aufnahmelogik ");
            if (srcdest.hasItem()) {
                Item HU = srcdest.getFirstItem();
                if (HU.getId().equals(wtorder.HU_Nummer) || this.controller.isIgnoreHuId()) { // cn1 korrekte HU ID
                                                                                              // Prüfung
                    srcdest.moveItem(this, HU, 0);
                    this.infoTG(HU, null);
                    if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                        this.moveOutMach();
                    }
                    blockedTransfer = false;
                    return;
                } else {
                    this.handleMMHU();
                    return;
                }
            } else { // if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                     // //gelöscht MTRE unabhängig von minWpcode_ProductionArea
                if (controller.isCreateHU() && lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                    core.logInfo(this,
                            "HU with HU_ID of WTSK will be created, since createHU field of controller is true");
                    Item extraItem1 = new Item();
                    extraItem1.setId(wtorder.HU_Nummer);
                    srcdest.addItem(null, extraItem1);
                }

                if (core.now() < timeEnd) {
                    zInfo_MTRE checkMtre = new zInfo_MTRE(core.now() + 1000);
                    checkMtre.setConveyor(srcdest);
                    checkMtre.setEndTime(timeEnd);
                    checkMtre.setFTF(this);
                    checkMtre.setProcess(process);
                    core.addEvent(checkMtre);

                    return;
                } else {
                    this.handleMTRE();
                    return;

                }
            }
            // }
        }

        blockedTransfer = false;

    }

    // Transfer auf Zielplatz
    public void handleDestTransfer(long timeEnd) {
        blockedTransfer = true;
        if (this.destMach != null) {
            core.logInfo(this, " jetzt Durchführung der HU Abgabelogik ");
            this.warteLogik();
            if (this.waitingActive) {
                // evtl boolean einbau inklusive handlestaufruf abfrage
                blockedTransfer = false;
                return;
            }
            if (this.hasItem()) {
                Item HU = this.getFirstItem();
                this.moveItem(destMach, HU, 0);
                this.wtcoTG(HU);
                // cn1 wtco Telegramm einbauen
                if (lastWaypointCode >= this.controller.getMinWpcode_ProductionArea()) {
                    Entity mapped = this.mapPaarbit(destMach, "VG", "02");
                    if (mapped != null) {
                        this.checkPaarbitMatch(mapped);
                    }
                    if (!paarbitActive) {
                        mapped = this.mapPaarbit(destMach, "VG", "04");
                        if (mapped != null) {
                            this.checkPaarbitMatch(mapped);
                        }
                        // 04 abfragem cn1,
                    }

                    this.moveOutMach();
                }
            } else if (core.now() < timeEnd) {
                zTransfer destEvent = new zTransfer(core.now() + 0);
                destEvent.setConveyor(destMach);
                destEvent.setEndTime(timeEnd);
                destEvent.setFTF(this);
                core.addEvent(destEvent);

            } else {

            }

        }
        blockedTransfer = false;
    }

    public Entity mapPaarbit(Entity destMach2, String codeEnding1, String codeEnding2) {// korrektes Mapping cn1
        // switch (destMach2.getId()) { // Wenn bestimmte Entitäten dann Abfrage
        String mach_r = destMach2.getId();

        String[] parts = mach_r.split("-");

        if (parts.length != 4) {
            return null;
        }

        // Kürzel und Endnummer abändern

        parts[2] = codeEnding1; // Parameter Einbau evtl
        parts[3] = codeEnding2; // Parameter Einbau evtl

        String combinedString = String.join("-", parts);

        try {
            Entity assignedVG = core.getEntityById(combinedString);
            return assignedVG;
        } catch (Exception e) {
            core.logInfo(this,
                    "No matching " + codeEnding1 + "-" + codeEnding2 + " Conveyor found for " + destMach2.getId());
        }
        return null;

    }

    public void infoTG(Item HU, String MFSError) {

        try {
            zTG1_INFO info = zTG1_INFO.getHeaderData();
            if (MFSError != null) {
                switch (MFSError) {
                    case "MMHU": // Falsche HU Nummer
                        info.HU_Nummer = wtorder.HU_Nummer;
                        info.Quelle = srcdest.getId();
                        info.CP = srcdest.getId();
                        break;
                    case "MPOE": // Zeit abgelaufen nach POSO
                        info.HU_Nummer = poso.HU_Nummer;
                        info.Quelle = posoSrc.getId();
                        info.CP = posoSrc.getId();
                        break;
                    case "MTRE": // nach kurzer Zeit trotz wtsk kein item auf Förderer
                        info.HU_Nummer = wtorder.HU_Nummer;
                        info.Quelle = srcdest.getId();
                        info.CP = srcdest.getId();
                        break;

                }

                info.MFS_Error = MFSError;
                info.Reserve = "...";

            } else {
                info.Quelle = destMach.getId();
                info.CP = destMach.getId(); // Nachfragen
                if (!this.controller.isIgnoreHuId()) {
                    info.HU_Nummer = HU.getId();
                } else {
                    info.HU_Nummer = this.wtorder.HU_Nummer;
                    core.logInfo(this, "For INFO telegram, HU Number of WTSK was used, since ignoreHuId is activated");
                }
                info.MFS_Error = "";
                info.Reserve = "...";
            }
            info.Endekennzeichen = zTG1.TELEGRAM_DELIMITER_ENDING;
            this.sendTelegram(info);
        } catch (Exception e) {
            core.logError(this, "Problem bei INFO Telegramm Erstellung " + e);
            return;
        }
    }

    public void wtcoTG(Item HU) {
        try {
            zTG1_WTCO conf = zTG1_WTCO.getHeaderData();
            conf.Quelle = this.srcdest.getId();
            conf.Ziel = this.destMach.getId();
            if (!this.controller.isIgnoreHuId()) {
                conf.HU_Nummer = HU.getId();
            } else {
                conf.HU_Nummer = this.wtorder.HU_Nummer;
                core.logInfo(this, "For WTCO telegram, HU Number of WTSK was used, since ignoreHuId is activated");
            }
            conf.HU_Höhe = wtorder.HU_Höhe; // unsicher ob Info auch aus Antsim entnehmbar
            conf.Paarbit = wtorder.Paarbit;
            conf.Paarbit = wtorder.Prioritätsbit;

            if (conf.Paarbit.equals("X")) {
                this.paarbitActive = false;
            }

            conf.MFS_Error = "";
            conf.Reserve = "";
            conf.Endekennzeichen = zTG1.TELEGRAM_DELIMITER_ENDING;
            this.sendTelegram(conf);

        } catch (Exception e) {
            core.logError(this, "Problem bei WTCO Telegramm Erstellung " + e);
            return;
        }
    }

    public void sendTelegram(zTG1 tg) {
        if (this.controller != null) {
            this.controller.sendTelegram(tg, this);
        }
    }

    public void handleMPOE(int WP) {
        if (this.isAt(WP)) {
            this.controller.FTFOrderpast.remove(this.poso); // wichtig um FTF endgültig aus Zuweisung zu entfernen
            this.moveFTF(to);
            this.infoTG(null, "MPOE");
            this.poso = null;
            this.posoSrc = null;
        }
    }

    public void handleMTRE() {
        this.infoTG(null, "MTRE");
        this.poso = null;
        this.wtorder = null;
        DestinationWay1.clear();
        blockedTransfer = false;
    }

    public void handleMMHU() {
        this.infoTG(null, "MMHU");
        this.poso = null;
        this.wtorder = null;
        DestinationWay1.clear();
        blockedTransfer = false;
    }

    public void checkPaarbitMatch(Entity mapped) {

        for (Map.Entry<zTG1_WTSK, Entity> entry : controller.paarbitWtsk.entrySet()) {
            if (entry.getValue() == mapped) { // richtiger Eintrag gefunden
                if (mapped.hasItem()) {
                    this.paarbitActive = true;
                    setWTSKOrder(entry.getKey());
                } else {
                    core.logError(this, "Paarbit Source Conveyor does not have HU stored, WTSK "
                            + entry.getValue()
                            + " will tried to be executed again (regularly) after Expiration of Paarbit checktime");
                }
                controller.paarbitWtsk.remove(entry.getKey());

            }

        }
    }

    public void warteLogik() {
        if (this.destMach == core.getEntityById(controller.getWartePlatz())) {
            waitingActive = true;
            this.DestinationWay1.clear();
        }
    }

    public zFTS_Waypoint getHomewp() {
        return to;
    }

    public String getFleetId() {
        return fleetId;
    }

    public zTG1_POSO getPoso() {
        return poso;
    }

    public int getLastWaypointCode() {
        return lastWaypointCode;
    }

    public void setLastWaypointCode(int lastWaypointCode) {
        this.lastWaypointCode = lastWaypointCode;

    }
}
