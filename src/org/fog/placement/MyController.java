package org.fog.placement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.MyApplication;
import org.fog.entities.FogDevice;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

public class MyController extends SimEntity {

	public static boolean ONLY_CLOUD = false;

	private List<MyFogDevice> fogDevices;
	private List<MySensor> sensors;
	private List<MyActuator> actuators;
	private static Map<Integer, Pair<Double, Integer>> mobilityMap;
	private Map<String, MyApplication> applications;
	private Map<String, Integer> appLaunchDelays;
	static Map<Integer, Integer> clusterInfo = new HashMap<Integer, Integer>();
	static Map<Integer, List<Integer>> clusters = new HashMap<Integer, List<Integer>>();

	private Map<String, MyModulePlacement> appModulePlacementPolicy;

	public MyController(String name, List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators) {
		super(name);
		this.applications = new HashMap<String, MyApplication>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, MyModulePlacement>());
		for (MyFogDevice fogDevice : fogDevices) {
			fogDevice.setControllerId(getId());
		}
		setMyFogDevices(fogDevices);
		setMyActuators(actuators);
		setMySensors(sensors);
		connectWithLatencies();
		// gatewaySelection();
		formClusters();
	}

	private MyFogDevice getMyFogDeviceById(int id) {
		for (MyFogDevice fogDevice : getMyFogDevices()) {
			if (id == fogDevice.getId())
				return fogDevice;
		}
		return null;
	}

	private void connectWithLatencies() {
		for (MyFogDevice fogDevice : getMyFogDevices()) {
			MyFogDevice parent = getMyFogDeviceById(fogDevice.getParentId());
			if (parent == null)
				continue;
			double latency = fogDevice.getUplinkLatency();
			parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			parent.getChildrenIds().add(fogDevice.getId());
		}
	}

	@Override
	public void startEntity() {
		for (String appId : applications.keySet()) {
			if (getAppLaunchDelays().get(appId) == 0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);

		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);

		for (MyFogDevice dev : getMyFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

		scheduleMobility();

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			System.exit(0);
			break;
		case FogEvents.FutureMobility:
			manageMobility(ev);
			break;

		}
	}

	private void printNetworkUsageDetails() {
		System.out
				.println("Total network usage = " + NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME);
	}

	private MyFogDevice getCloud() {
		for (MyFogDevice dev : getMyFogDevices())
			if (dev.getName().equals("cloud"))
				return dev;
		return null;
	}

	private void printCostDetails() {
		System.out.println("Cost of execution in cloud = " + getCloud().getTotalCost());
	}

	private void printPowerDetails() {
		for (MyFogDevice fogDevice : getMyFogDevices()) {
			System.out.println(fogDevice.getName() + " : Energy Consumed = " + fogDevice.getEnergyConsumption());
		}
	}

	private String getStringForLoopId(int loopId) {
		for (String appId : getMyApplications().keySet()) {
			MyApplication app = getMyApplications().get(appId);
			for (AppLoop loop : app.getLoops()) {
				if (loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}

	private void printTimeDetails() {
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		System.out.println("=========================================");
		System.out.println("EXECUTION TIME : "
				+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		System.out.println("APPLICATION LOOP DELAYS");
		System.out.println("=========================================");
		for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {

			System.out.println(getStringForLoopId(loopId) + " ---> "
					+ TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
		}
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");

		for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
			System.out.println(
					tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}

		System.out.println("=========================================");
	}

	protected void manageResources() {
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}

	private void processTupleFinished(SimEvent ev) {
	}

	@Override
	public void shutdownEntity() {
	}

	public void submitApplication(MyApplication application, int delay, MyModulePlacement modulePlacement) {
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);

		for (MySensor sensor : sensors) {
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for (MyActuator ac : actuators) {
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}

		for (AppEdge edge : application.getEdges()) {
			if (edge.getEdgeType() == AppEdge.ACTUATOR) {
				String moduleName = edge.getSource();
				for (MyActuator actuator : getMyActuators()) {
					if (actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(),
								edge.getTupleType());
				}
			}
		}
	}

	public void submitApplication(MyApplication application, MyModulePlacement modulePlacement) {
		submitApplication(application, 0, modulePlacement);
	}

	private void processAppSubmit(SimEvent ev) {
		MyApplication app = (MyApplication) ev.getData();
		processAppSubmit(app);
	}

	private void processAppSubmit(MyApplication application) {
		System.out.println(CloudSim.clock() + " Submitted application " + application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);

		MyModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for (MyFogDevice fogDevice : fogDevices) {
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}

		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();

		for (Integer deviceId : deviceToModuleMap.keySet()) {
			for (AppModule module : deviceToModuleMap.get(deviceId)) {

				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				System.out.println(CloudSim.clock() + " Trying to Launch " + module.getName() + " in "
						+ getMyFogDeviceById(deviceId).getName());
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}

	}

	public List<MyFogDevice> getMyFogDevices() {
		return fogDevices;
	}

	public void setMyFogDevices(List<MyFogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, MyApplication> getMyApplications() {
		return applications;
	}

	public void setMyApplications(Map<String, MyApplication> applications) {
		this.applications = applications;
	}

	public List<MySensor> getMySensors() {
		return sensors;
	}

	public void setMySensors(List<MySensor> sensors) {
		for (MySensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<MyActuator> getMyActuators() {
		return actuators;
	}

	public void setMyActuators(List<MyActuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, MyModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, MyModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}

	public void setMobilityMap(Map<Integer, Pair<Double, Integer>> mobilityMap) {
		this.mobilityMap = mobilityMap;
	}

	private void scheduleMobility() {
		for (int id : mobilityMap.keySet()) {
			Pair<Double, Integer> pair = mobilityMap.get(id);
			double mobilityTime = pair.getFirst();
			int mobilityDestinationId = pair.getSecond();
			Pair<Integer, Integer> newConnection = new Pair<Integer, Integer>(id, mobilityDestinationId);
			send(getId(), mobilityTime, FogEvents.FutureMobility, newConnection);
		}
	}

	private void manageMobility(SimEvent ev) {
		Pair<Integer, Integer> pair = (Pair<Integer, Integer>) ev.getData();
		int deviceId = pair.getFirst();
		int newParentId = pair.getSecond();
		MyFogDevice deviceWithMobility = getMyFogDeviceById(deviceId);
		MyFogDevice mobilityDest = getMyFogDeviceById(newParentId);
		deviceWithMobility.setParentId(newParentId);
		System.out.println(CloudSim.clock() + " " + deviceWithMobility.getName() + " is now connected to "
				+ mobilityDest.getName());
	}

	private void gatewaySelection() {
		// TODO Auto generated method stub
		for (int i = 0; i < getMyFogDevices().size(); i++) {
			MyFogDevice fogDevice = getMyFogDevices().get(i);
			int parentID = -1;
			if (fogDevice.getParentId() == -1) {
				double minDistance = Config.MAX_NUMBER;
				for (int j = 0; j < getMyFogDevices().size(); j++) {
					MyFogDevice anUpperDevice = getMyFogDevices().get(j);
					if (fogDevice.getLevel() + 1 == anUpperDevice.getLevel()) {
						double distance = calculateDistance(fogDevice, anUpperDevice);
						if (distance < minDistance) {
							minDistance = distance;
							parentID = anUpperDevice.getId();
						}
					}
				}
			}
			fogDevice.setParentId(parentID);
		}
	}

	private double calculateDistance(MyFogDevice fogDevice, MyFogDevice anUpperDevice) {

		return Math.sqrt(Math.pow(fogDevice.getxCoordinate() - anUpperDevice.getxCoordinate(), 2.00)
				+ Math.pow(fogDevice.getyCoordinate() - anUpperDevice.getyCoordinate(), 2.00));
	}

	private void formClusters() {
		for (MyFogDevice fd : getMyFogDevices()) {
			clusterInfo.put(fd.getId(), -1);
		}

		int clusterId = 0;

		// Percorar todos os nós
		for (int i = 0; i < getMyFogDevices().size(); i++) {
			// Recupere o nó'atual
			MyFogDevice fd1 = getMyFogDevices().get(i);
			// Percorra novamente os nós
			for (int j = 0; j < getMyFogDevices().size(); j++) {
				// Recupere o nó atual
				MyFogDevice fd2 = getMyFogDevices().get(j);

				// Se fd1 <> fd2, os pais são o mesmo nó, a distância entre os
				// dois nós é menor que o estabelecido e os nós estão no mesmo
				// nível
				if (fd1.getId() != fd2.getId() && fd1.getParentId() == fd2.getParentId()
						&& calculateDistance(fd1, fd2) < Config.CLUSTER_DISTANCE && fd1.getLevel() == fd2.getLevel()) {
					int fd1ClusteriD = clusterInfo.get(fd1.getId());
					int fd2ClusteriD = clusterInfo.get(fd2.getId());
					if (fd1ClusteriD == -1 && fd2ClusteriD == -1) {
						clusterId++;
						clusterInfo.put(fd1.getId(), clusterId);
						clusterInfo.put(fd2.getId(), clusterId);
					} else if (fd1ClusteriD == -1)
						clusterInfo.put(fd1.getId(), clusterInfo.get(fd2.getId()));
					else if (fd2ClusteriD == -1)
						clusterInfo.put(fd2.getId(), clusterInfo.get(fd1.getId()));
				}
			}
		}

		// Percorra os nós do cluster
		for (int id : clusterInfo.keySet()) {
			if (!clusters.containsKey(clusterInfo.get(id))) {
				List<Integer> clusterMembers = new ArrayList<Integer>();
				clusterMembers.add(id);
				clusters.put(clusterInfo.get(id), clusterMembers);
			} else {
				List<Integer> clusterMembers = clusters.get(clusterInfo.get(id));
				clusterMembers.add(id);
				clusters.put(clusterInfo.get(id), clusterMembers);
			}
		}
		for (int id : clusters.keySet()) {
			System.out.println(id + " " + clusters.get(id));
		}
	}

}