package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.MyApplication;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;

public class MyModulePlacement extends MyPlacement {

	protected ModuleMapping moduleMapping;
	protected List<MySensor> sensors;
	protected List<MyActuator> actuators;
	protected String moduleToPlace;
	protected List<String> modulesToPlace = new ArrayList<String>();
	protected Map<Integer, Integer> deviceMipsInfo;

	public MyModulePlacement(List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators,
			MyApplication application, ModuleMapping moduleMapping, String moduleToPlace) {
		this.setMyFogDevices(fogDevices);
		this.setMyApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		setMySensors(sensors);
		setMyActuators(actuators);
		this.moduleToPlace = moduleToPlace;
		this.deviceMipsInfo = new HashMap<Integer, Integer>();
		mapModules();
	}

	public MyModulePlacement(List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators,
			MyApplication application, ModuleMapping moduleMapping) {
		this.setMyFogDevices(fogDevices);
		this.setMyApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		setMySensors(sensors);
		setMyActuators(actuators);
		this.deviceMipsInfo = new HashMap<Integer, Integer>();
		mapModulesCloud();
	}

	public void mapModulesCloud() {

		for (String deviceName : getModuleMapping().getModuleMapping().keySet()) {
			for (String moduleName : getModuleMapping().getModuleMapping().get(deviceName)) {
				int deviceId = CloudSim.getEntityId(deviceName);
				AppModule appModule = getMyApplication().getModuleByName(moduleName);
				if (!getDeviceToModuleMap().containsKey(deviceId)) {

					// Se o módulo não está no dispositivo, o adicione
					List<AppModule> placedModules = new ArrayList<AppModule>();
					placedModules.add(appModule);
					getDeviceToModuleMap().put(deviceId, placedModules);

				} else {
					List<AppModule> placedModules = getDeviceToModuleMap().get(deviceId);
					placedModules.add(appModule);

					getDeviceToModuleMap().put(deviceId, placedModules);
				}
			}
		}
		
		MyApplication app = getMyApplication();
		List<AppModule> placedModules = new ArrayList<AppModule>();
		for (AppModule module : app.getModules()) {

			//placedModules.add(module);

		}
		//getDeviceToModuleMap().put(getDeviceByName("cloud").getId(), placedModules);

	}

	@Override
	protected void mapModules() {

		// Esse laço percorre todos os nós fogs e módulos, verifica se já existe
		// mapeamento para o módulo
		// ou dispositivo e os inclui em listas indicando.

		for (String deviceName : getModuleMapping().getModuleMapping().keySet()) {
			for (String moduleName : getModuleMapping().getModuleMapping().get(deviceName)) {
				int deviceId = CloudSim.getEntityId(deviceName);
				AppModule appModule = getMyApplication().getModuleByName(moduleName);
				if (!getDeviceToModuleMap().containsKey(deviceId)) {

					// Se o módulo não está no dispositivo, o adicione
					List<AppModule> placedModules = new ArrayList<AppModule>();
					placedModules.add(appModule);
					getDeviceToModuleMap().put(deviceId, placedModules);

				} else {
					List<AppModule> placedModules = getDeviceToModuleMap().get(deviceId);
					placedModules.add(appModule);

					getDeviceToModuleMap().put(deviceId, placedModules);
				}
			}
		}

		// Percorre a lista de todos os fog devices
		for (MyFogDevice device : getMyFogDevices()) {
			int deviceParent = -1;

			// Lista que receberá os ids de todos on nós
			List<Integer> children = new ArrayList<Integer>();

			//
			if (device.getLevel() == 1) {
				if (!deviceMipsInfo.containsKey(device.getId()))
					deviceMipsInfo.put(device.getId(), 0);
				deviceParent = device.getParentId();
				for (MyFogDevice deviceChild : getMyFogDevices()) {
					if (deviceChild.getParentId() == device.getId()) {
						children.add(deviceChild.getId());
					}
				}

				Map<Integer, Double> childDeadline = new HashMap<Integer, Double>();
				for (int childId : children)
					childDeadline.put(childId, getMyApplication().getDeadlineInfo().get(childId).get(moduleToPlace));

				List<Integer> keys = new ArrayList<Integer>(childDeadline.keySet());

				for (int i = 0; i < keys.size() - 1; i++) {
					for (int j = 0; j < keys.size() - i - 1; j++) {

						// Verifica o deadline do nó atual com o proximo nó de e
						// os ordena.
						if (childDeadline.get(keys.get(j)) > childDeadline.get(keys.get(j + 1))) {
							int tempJ = keys.get(j);
							int tempJn = keys.get(j + 1);
							keys.set(j, tempJn);
							keys.set(j + 1, tempJ);
						}
					}
				}

				// Variável baseMipsOfPlacingModule recebe a necessidade em mips
				// para um
				// determinado módulo de aplicação
				int baseMipsOfPlacingModule = (int) getMyApplication().getModuleByName(moduleToPlace).getMips();

				for (int key : keys) {
					// Recebe a capacidade atual do dispositivo
					int currentMips = deviceMipsInfo.get(device.getId());

					// Recebe o módulo e adiciona à variável additionalMips a
					// necessidade de cada
					// módulo
					AppModule appModule = getMyApplication().getModuleByName(moduleToPlace);
					int additionalMips = getMyApplication().getAdditionalMipsInfo().get(key).get(moduleToPlace);

					// Verifica se o módulo cabe no dispositivo.
					if (currentMips + baseMipsOfPlacingModule + additionalMips < device.getMips()) {

						currentMips = currentMips + baseMipsOfPlacingModule + additionalMips;

						// Adiciona o novo status de capacidade ao dispositivo
						deviceMipsInfo.put(device.getId(), currentMips);

						// Returns true if this map contains a mapping for the
						// specified key.
						// More formally, returns true if and only if this map
						// contains a mapping for
						// a key k such that (key==null ? k==null :
						// key.equals(k)). (There can be at
						// most one such mapping.)
						if (!getDeviceToModuleMap().containsKey(device.getId())) {
							List<AppModule> placedModules = new ArrayList<AppModule>();
							placedModules.add(appModule);
							getDeviceToModuleMap().put(device.getId(), placedModules);

						} else {
							List<AppModule> placedModules = getDeviceToModuleMap().get(device.getId());
							placedModules.add(appModule);
							getDeviceToModuleMap().put(device.getId(), placedModules);
						}
					} else {

						// Senão, envie para o cara de cima da topologia
						List<AppModule> placedModules = getDeviceToModuleMap().get(deviceParent);
						placedModules.add(appModule);
						getDeviceToModuleMap().put(deviceParent, placedModules);
					}
				}

			}

		}

	}

	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	public List<MySensor> getMySensors() {
		return sensors;
	}

	public void setMySensors(List<MySensor> sensors) {
		this.sensors = sensors;
	}

	public List<MyActuator> getMyActuators() {
		return actuators;
	}

	public void setMyActuators(List<MyActuator> actuators) {
		this.actuators = actuators;
	}

}
