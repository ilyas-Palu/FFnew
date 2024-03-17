package com.ssn.simulation.plugin.zFTS1;

import java.util.ArrayList;
import java.util.List;

import com.ssn.simulation.builders.EntityBuilder;
import com.ssn.simulation.core.Entity;
import com.ssn.simulation.editor.EntityConverter;
import com.ssn.simulation.functions.UserFunction;

public class PluginMain
		implements com.ssn.simulation.plugin.Plugin {

	@Override
	public String getId() {
		return "zFTS";
	}

	@Override
	public List<Class<? extends Entity>> registerEntities() {
		List<Class<? extends Entity>> list = new ArrayList<Class<? extends Entity>>();
		list.add(zFTS1.class);
		list.add(ZFTS_Connector.class);
		list.add(zFTS_Entity1.class);
		list.add(zFTS_Waypoint.class);
		return list;
	}

	@Override
	public List<UserFunction> registerUserFunctions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<EntityBuilder> registerEntityBuilders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<EntityConverter> registerEntityConverters() {
		// TODO Auto-generated method stub
		return null;
	}

}
