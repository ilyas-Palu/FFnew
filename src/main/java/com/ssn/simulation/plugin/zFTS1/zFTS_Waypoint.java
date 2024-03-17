package com.ssn.simulation.plugin.zFTS1;

import java.util.ArrayList;
import java.util.List;

import com.ssn.simulation.core.Entity;
import com.ssn.simulation.entities.BinWeaselRoutingStrategy;

public class zFTS_Waypoint extends Entity {

    private static final long serialVersionUID = 1L;

    protected String fleetId;
    protected int waypointCode;
    protected double maxSpeed;
    protected List<BinWeaselRoutingStrategy> routingStrategies;
    protected String matchEntity;

    protected transient zFTS1 controller;

    public zFTS_Waypoint() {
        super();
        sizex = 0.1;
        sizey = 0.1;
        sizez = 0.1;
        transparent = true;
        fleetId = "11011";
        waypointCode = 0;
        maxSpeed = 1.0;
        routingStrategies = new ArrayList<>();
        routingStrategies.add(new BinWeaselRoutingStrategy("*", 0));
        matchEntity = "";
    }

    @Override
    public String getCategory() {
        return "FTS";
    }

    @Override
    public Object clone() {
        try {
            zFTS_Waypoint entity = (zFTS_Waypoint) super.clone();
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
        for (Entity entity : core.getEntities()) {
            if (entity instanceof zFTS1) {
                zFTS1 crtl = (zFTS1) entity;
                if (crtl.getFleetID().equals(fleetId)) {
                    controller = crtl;
                }
            }
        }
    }

    @Override
    public void onAttributesChanged() {
        super.onAttributesChanged();
        setStringProperty("fleetId", fleetId, "FTS Waypoint");
        setStringProperty("matchingEntity", matchEntity, "FTS Waypoint");
        setIntegerProperty("waypointCode", waypointCode, "FTS Waypoint");
        setDoubleProperty("maxSpeed", maxSpeed, 0.1, "FTS Waypoint");
        setListProperty("routingStrategies", routingStrategies, "FTS Waypoint",
                createPojoListProperty(BinWeaselRoutingStrategy.class));
    }

    @Override
    public void onPropertiesChanged() {
        super.onPropertiesChanged();
        fleetId = getStringProperty("fleetId");
        matchEntity = getStringProperty("matchingEntity");
        waypointCode = getIntegerProperty("waypointCode");
        routingStrategies = getListProperty("routingStrategies", BinWeaselRoutingStrategy.class);
    }

    public String getFleetId() {
        return fleetId;
    }

    public int getWaypointCode() {
        return waypointCode;
    }

    public zFTS_Waypoint nextWaypoint(int destination) { // Routing Logik anpassen/implementieren cn1
        if (destination != 0) {
            if (destination == waypointCode) {
                return null;
            }
            for (BinWeaselRoutingStrategy strategy : routingStrategies) {
                if (strategy.match(destination)) {
                    Entity entity = getOutputEntity(strategy.getPort());
                    if (entity instanceof zFTS_Waypoint) {
                        return (zFTS_Waypoint) entity;
                    } else {
                        core.logError(this, /// evtl hier Verbindung zu Standart Entitäten handlen cn1
                                "invalid output (" + strategy.getPort() + ") port for destination " + destination);
                        return null;
                    }
                }
            }
        }
        // core.logError(this, "unable to find a valid output port for destination " +
        // destination);
        return null;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public String getMatchEntity() {
        return matchEntity;
    }

    public void setMatchEntity(String matchEntity) {
        this.matchEntity = matchEntity;
    }

}
