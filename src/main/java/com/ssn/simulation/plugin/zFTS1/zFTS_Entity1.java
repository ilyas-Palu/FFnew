package com.ssn.simulation.plugin.zFTS1;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.core.Item;

public class zFTS_Entity1 extends Entity {

    // real Entity of FTS (about 20 needed in Model)

    protected String fleetId;
    protected int startPosition; // Waypoint where FTS starts at runtime
    protected int homePosition;
    protected double travelSpeed;
    protected double vehicleDistance;

    protected transient zFTS1 controller; // Ersetzen durch eigenen Controller
    protected transient int lastWaypointCode; // used ip
    protected transient Entity destMach; // used

    protected transient Entity srcdest; // used

    protected transient Entity posoSrc; // used

    protected transient Set<zFTS_Waypoint> allWaypoints; // used
    protected transient zFTS_Waypoint from; // use like lastwaypointcode
    protected transient zFTS_Waypoint to; // currently probaby home waypoint

    protected transient Map<String, zFTS_Waypoint> DestinationWay1; // Alle Destinations per Code
    protected transient zTG1_POSO poso;
    protected transient zTG1_WTSK wtorder;
    protected transient long posoTime;
    protected transient boolean assigned = false;
    protected transient boolean blockedTransfer = false;

    public zFTS_Entity1() {
        sizex = 0.7;
        sizey = 0.7;
        fleetId = "121212";
        startPosition = 1;
        homePosition = 1;
        travelSpeed = 1.0;
        vehicleDistance = 1.0;
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
                    if (waypoint.getWaypointCode() == startPosition) { // Zuweisen über startposition Code!
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
            // handleWeaselWithoutOrder(to); // Eventuell irrelevant, aber nachpflegen
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
        // arrive way point
        // Abfrage ob Ziel gespeichert
        if (!isError()) {
            if (!isMoving()) {
                if (!blockedTransfer) {
                    if (!DestinationWay1.isEmpty()) {
                        // if (this.to.getWaypointCode() == 1) { // Allgemeinere Prozesslogik für WTSK
                        Entry<String, zFTS_Waypoint> entry = DestinationWay1.entrySet().iterator().next();
                        zFTS_Waypoint zwp = entry.getValue();
                        String vProcess = entry.getKey();
                        // Abfrage ob Waypointcodes passen und diese noch nicht erreicht
                        if (vProcess.equals("POSO")) { // besser wieder ändern auf poso.subtype
                            this.posoTime = core.now();
                            if ((zwp.getWaypointCode() == 11 || zwp.getWaypointCode() == 12
                                    || zwp.getWaypointCode() == 13) && this.isAt(to)) { // geändete von 1 auf to
                                core.logInfo(this, "POSO wurde erkannt");
                                this.moveWeasel(zwp);
                                lastWaypointCode = zwp.getWaypointCode(); // Übergabe aktuelle Position
                                from = zwp; // eig Rendundanz mit lastwaypointcode, evtl nur from Nutzung
                                return;
                            }

                            if (!this.posoSrc.hasItem()) {
                                if (!this.posoSrc.hasItem()) {
                                    zInfo_MPOE checkEvent = new zInfo_MPOE(core.now() + 10000);
                                    checkEvent.setConveyor(posoSrc);
                                    checkEvent.setWP(zwp.getWaypointCode());
                                    checkEvent.setFTF(this);
                                    core.addEvent(checkEvent);
                                }
                            }

                            this.DestinationWay1.remove(vProcess);// besser eher am Ende der Methode 1mal

                            // ausführen
                            // poso = null; //unsicher cn1
                            return;

                            // }
                        }

                        if (this.wtorder != null) {
                            if (vProcess.contains("WTSK")) {
                                // if (this.isAt(from)) { // Sicherheitsabfrage bzw. auch ob auf einem Waypoint
                                // überhaupt
                                // Wtorder Logik -> Schritt 1 destwp (mittig) beauftragen/fahren
                                if (from == zwp) { // lastdest erreicht (bzw. Quellplatz)
                                    if (vProcess.contains("Q")) {
                                        if (lastWaypointCode > 50) {// ->im Produktionsnetz
                                            // Entity moveProd = this.calcproddest(); // vermutlich obsolet cn1
                                            // if (moveProd instanceof zFTS_Waypoint) {
                                            // zFTS_Waypoint wpdir = (zFTS_Waypoint) moveProd;
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
                                        // }
                                        // INFO Telegramm
                                    } else {
                                        if (lastWaypointCode > 50) { // evtl obsolete/unvollständige Abfrage -> Auch
                                                                     // Transfer außerhalb
                                                                     // wpnetz möglich!
                                            this.moveproddestx(vProcess, false);
                                            if (!isMoving()) {
                                                this.moveproddesty(vProcess, false);
                                            } else {
                                                return;
                                            }

                                        }
                                        if (!isMoving()) {
                                            handleDestTransfer(core.now() + 30000);
                                        } else {
                                            return;
                                        }

                                        // item transfer cn1

                                        // Item Transfer an normale Fördertechnik cn1

                                        // WTCO Telegramm

                                        // evtl Einbau clear WTOrder
                                        this.wtorder = null;
                                        poso = null;
                                    }
                                    this.DestinationWay1.remove(vProcess);
                                    // löschen Eintrag
                                    return;
                                } else {
                                    if (this.isAt(from)) { // ->Routing möglich
                                        if (from.nextWaypoint(zwp.getWaypointCode()) != null) {
                                            from = from.nextWaypoint(zwp.getWaypointCode());
                                            core.logInfo(this,
                                                    "Waypoint Routing erfolgreich Beauftragung nach " + from.getId());
                                            this.moveWeasel(from);
                                            lastWaypointCode = from.getWaypointCode();
                                            return;
                                        }
                                    } else {
                                        if (lastWaypointCode > 50) {
                                            if (!isMoving()) {
                                                moveWeasel(from);
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
                                // moveWeasel(to); // hier routing zum nächsten WP einbauen wie oben bei wtsk !
                                if (from.nextWaypoint(to.getWaypointCode()) != null) { // rendundante logik mit wtsk
                                                                                       // evtl
                                                                                       // auslagern cn1
                                    from = from.nextWaypoint(to.getWaypointCode());
                                    core.logInfo(this,
                                            "Waypoint Routing erfolgreich, Beauftragung nach" + from.getId());
                                    this.moveWeasel(from);
                                    lastWaypointCode = from.getWaypointCode();
                                    return;
                                }
                            } else {
                                this.moveOutMach();
                                if (!this.isMoving()) {

                                    moveWeasel(from);
                                }
                            }
                            return;
                        }
                    }
                    // if (this.isAt(to) && this.wtorder != null) {
                    // this.wtorder = null; //cn1 unsicher ob Funktionalität gesichert
                    // }
                }
            }
        }
    }

    @Override
    public void onMoved(double x, double y, double z) {
        super.onMoved(this.getPosx(), this.getPosy(), this.getPosz());
        setMoving(false);

    }
    // Logik welche eventuell über alle zWaypoint Instanzen iteriert mit isAt
    // Abfrage -> Herausfinden ob waypointfreie Logik ausgeführt wird
    // }

    public void moveproddestx(String ent, boolean rev) {
        float prx = 0;
        float pry = 0;
        if ((ent.contains("Z") && !rev) || ((!ent.contains("Z")) && rev)) {
            prx = (float) this.destMach.getPosx();
            pry = (float) this.destMach.getPosy(); // y eig irrelevant
        } else {
            prx = (float) this.srcdest.getPosx();
            pry = (float) this.srcdest.getPosy();
        }
        if (rev) { // cn1 evtl durch switch ersetzen
            prx = (float) this.from.getPosx();
        }
        if (this.posx - prx < 0.03 && this.posx - prx > -0.03) { // Abfrage ob Position bereits stimmt
            return;
        }

        // setMoving(true);
        this.moveWithSpeed(prx, from.getPosy(), destMach.getPosz(), 0);

        // Zwischenkoordinaten als Array z.B. in Objekt speichern und in onNotify
        // abfragen
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

        if (rev) { // cn1 unbedingt durch switch ersetzen
            pry = (float) this.from.getPosy();
        }

        if (pry > this.getPosy()) { // Anpassung mitsamt eigener Größe für Berechnung
            buf1 = pry - buf1;
        } else {
            buf1 = pry + buf1;
        }

        if (this.posy - buf1 < 0.03 && posy - buf1 > -0.03 || rev) { // Abfrage ob Position bereits stimmt
            return;
        }
        // setMoving(true);
        this.moveWithSpeed(prx, buf1, from.getPosz(), 0);

        // Zwischenkoordinaten als Array z.B. in Objekt speichern und in onNotify
        // abfragen
    }

    public void moveWithSpeed(double x1, double y1, double z1, long t1) {

        // double tv = this.travelSpeed;

        double realDistanceX = Math.abs(this.posx - x1); // Ergebniss immer positiv (Betrag)
        double realDistanceY = Math.abs(this.posy - y1); // Ergebniss immer positiv (Betrag)
        // bisher keine Z Betrachtung
        double totalDistance = realDistanceX + realDistanceY;

        long timeNeeded = ((long) (totalDistance / travelSpeed)) * 1000; // in s

        setMoving(true);

        moveEntity(x1, y1, z1, timeNeeded);

    }

    public void moveWeasel(zFTS_Waypoint waypoint) {
        // if (maxDistance > distanceToWaypoint) {// cn1 not reversed operator
        // starke Abweichung von Referenzentitäten !
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

        if (this.posy == from.getPosy()) {// cn1 hier vermutlich falsch
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
        core.logInfo(this, "Beauftragung wtsk auf Förderer " + lastdest1 + " wegen Ziel " + destMach);
        if (firstsrc != null) {
            DestinationWay1.put("WTSK-Q", firstsrc); // eigentliche Quelle zuerst
        } else {
            // Fehlerbehandlung
            return;
        }
        DestinationWay1.put("WTSK-Z", lastdest1); // eigentliches Ziel

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

        // double zxp = zDest.getPosx();
        // double zxy = zDest.getPosy();

        zFTS_Waypoint mindist = null;
        float dist1 = 99999; // Sauberer: positive unendlichkeit einbauen
        float comp1 = 0;

        for (zFTS_Waypoint element : allWaypoints) {

            // Durch Waypoints iterieren Koordinaten speichern und nähsten Zurückgeben
            comp1 = (float) zDest.distanceTo(element);
            if (element.getWaypointCode() == 61 || element.getWaypointCode() == 64 || element.getWaypointCode() == 67
                    || element.getWaypointCode() == 70 || element.getWaypointCode() == 73) {
                if (comp1 < dist1) {
                    dist1 = comp1;
                    mindist = element;
                }
            }

            // if (dist1 < mindist) {
            // ele6 = dist1;
        }

        return mindist;

    }

    public boolean hasDestination() {
        return !this.DestinationWay1.isEmpty();
    }

    // return mindist;

    public void setAssigned(boolean b) {
        this.assigned = b;
    }

    public void handleSrcTransfer(long timeEnd, String process) { // Allgemeine HU Transfer Methode auf FTF
        blockedTransfer = true;
        if (this.srcdest != null) {
            core.logInfo(this, " jetzt Durchführung der HU Aufnahmelogik ");
            if (srcdest.hasItem()) {
                Item HU = srcdest.getFirstItem();
                if (HU.getId().equals(wtorder.HU_Nummer)) { // cn1 korrekte HU ID Prüfung
                    srcdest.moveItem(this, HU, 0);
                    this.infoTG(HU, null);
                    if (lastWaypointCode > 50) {
                        this.moveOutMach();
                    }
                    blockedTransfer = false;
                    return;
                } else {
                    this.handleMMHU();
                }
                // }
            } else if (lastWaypointCode > 50) {
                if (core.now() < timeEnd) {
                    zInfo_MTRE checkMtre = new zInfo_MTRE(core.now() + 1000);
                    checkMtre.setConveyor(srcdest);
                    checkMtre.setEndTime(timeEnd);
                    checkMtre.setFTF(this);
                    checkMtre.setProcess(process);
                    core.addEvent(checkMtre);
                    // warten auf Item Logik, evtl über Event oder Notify Methode cn1
                    return;
                } else {
                    this.handleMTRE();

                }
            }
        }

        blockedTransfer = false;

        // this.infoTG(null,);

    }

    // Transfer auf Zielplatz
    public void handleDestTransfer(long timeEnd) { // time Änderung zu Endtime
        blockedTransfer = true;
        if (this.destMach != null) {
            core.logInfo(this, " jetzt Durchführung der HU Abgabelogik ");
            if (this.hasItem()) {
                Item HU = this.getFirstItem();
                this.moveItem(destMach, HU, 0);
                this.wtcoTG(HU);
                // cn1 wtco Telegramm einbauen
                if (lastWaypointCode > 50) {
                    Entity mapped = this.mapPaarbit(destMach); // mapping einbauen cn1
                    if (mapped != null) {
                        for (Map.Entry<Entity, zTG1_WTSK> entry : controller.paarbitWtsk.entrySet()) {
                            if (entry.getKey() == mapped) {
                                setWTSKOrder(entry.getValue());

                                // handleesrctransfer
                                // Ziel herausziehen und in Routingtabelle
                                // wtsk Eintrag korrekt aus Tabelle löschen
                            }

                        }
                    }
                    this.moveOutMach();
                }
            } else if (core.now() < timeEnd) {
                zTransfer destEvent = new zTransfer(core.now() + 0);
                destEvent.setConveyor(destMach);
                destEvent.setEndTime(timeEnd);
                destEvent.setFTF(this);
                core.addEvent(destEvent);
                // this.core.addEvent(new com.ssn.simulation.core.Event(this.core.now() +
                // 3000));
                // Logik warten ? cn1
            } else {

            }

        }
        blockedTransfer = false;
    }

    public Entity mapPaarbit(Entity destMach2) {// korrektes Mapping cn1
        switch (destMach2.getId()) { // Wenn bestimmte Entitäten dann Abfrage
            case "Halfauomatic":
                return destMach2;

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
                        info.Quelle = destMach.getId();
                        info.CP = destMach.getId();
                        break;
                    case "MPOE": // Zeit abgelaufen nach POSO
                        info.HU_Nummer = poso.HU_Nummer;
                        info.Quelle = posoSrc.getId();
                        info.CP = posoSrc.getId();
                        break;
                    case "MTRE": // nach kurzer Zeit trotz wtsk kein item auf Förderer
                        info.HU_Nummer = wtorder.HU_Nummer;
                        info.Quelle = destMach.getId();
                        info.CP = destMach.getId();
                        break;

                }

                info.MFS_Error = MFSError;
                info.Reserve = "...";

            } else {
                info.Quelle = destMach.getId();
                info.CP = destMach.getId(); // Nachfragen
                info.HU_Nummer = HU.getId();
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
            conf.HU_Nummer = HU.getId();
            conf.HU_Höhe = wtorder.HU_Höhe; // unsicher ob Info auch aus Antsim entnehmbar
            conf.Paarbit = wtorder.Paarbit;
            conf.Paarbit = wtorder.Prioritätsbit;
            conf.MFS_Error = "";
            conf.Reserve = "";
            conf.Endekennzeichen = zTG1.TELEGRAM_DELIMITER_ENDING;
            this.sendTelegram(conf);
            // evtl vollkommen obsolet da wtco fast genau gleich wie wtsk, vermutlich nur
            // anpassung bei error/fehler cn1

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
            this.moveWeasel(to);
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
