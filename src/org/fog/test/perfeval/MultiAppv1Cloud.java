package org.fog.test.perfeval;

//Até este ponto a aplicação executa na fog sem problemas utilizando uma abordagem de colocação fixa. 12/07/2018

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.MyApplication;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.entities.Tuple;
import org.fog.placement.ModuleMapping;
import org.fog.placement.MyController;
import org.fog.placement.MyModulePlacement;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class MultiAppv1Cloud {
	static List<MyFogDevice> fogDevices = new ArrayList<MyFogDevice>();
	static Map<Integer, MyFogDevice> deviceById = new HashMap<Integer, MyFogDevice>();
	static List<MySensor> sensors = new ArrayList<MySensor>();
	static List<MyActuator> actuators = new ArrayList<MyActuator>();
	static List<Integer> idOfEndDevices = new ArrayList<Integer>();
	static Map<Integer, Map<String, Double>> deadlineInfo = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo = new HashMap<Integer, Map<String, Integer>>();
	static List<MyFogDevice> mobiles = new ArrayList<MyFogDevice>();

	static boolean CLOUD = true;

	static int numOfGateways = 2;
	static int numOfEndDevPerGateway = 4;
	static double sensingInterval = 5;

	public static void main(String[] args) {

		Log.printLine("Starting TestApplication...");

		try {
			Log.disable();
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
			CloudSim.init(num_user, calendar, trace_flag);
			String appId = "test_app";
			FogBroker broker = new FogBroker("broker");

			createFogDevices(broker.getId(), appId);

			MyApplication application = createApplication0(appId, broker.getId());
			application.setUserId(broker.getId());

			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

			moduleMapping.addModuleToDevice("moduleD_1", "cloud");
			moduleMapping.addModuleToDevice("moduleC_1", "cloud");
			moduleMapping.addModuleToDevice("moduleB_1", "cloud");
			for (int i = 0; i < idOfEndDevices.size(); i++) {
				MyFogDevice fogDevice = deviceById.get(idOfEndDevices.get(i));
				moduleMapping.addModuleToDevice("moduleA_1", fogDevice.getName());
			}

			MyController controller = new MyController("master-controller", fogDevices, sensors, actuators);

			controller.submitApplication(application, 0,
					new MyModulePlacement(fogDevices, sensors, actuators, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("TestApplication finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	private static double getvalue(double min, double max) {
		Random r = new Random();
		double randomValue = min + (max - min) * r.nextDouble();
		return randomValue;
	}

	private static int getvalue(int min, int max) {
		Random r = new Random();
		int randomValue = min + r.nextInt() % (max - min);
		return randomValue;
	}

	private static void createFogDevices(int userId, String appId) {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw(i + "", userId, appId, cloud.getId());
		}

	}
	
	private static void createFogDevices() {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
		cloud.setParentId(-1);
		MyFogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Proxy Server (level=1)
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
		proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		for(int i=0;i<numOfGateways;i++){
			addGw(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}
		
	}

	private static void addGw(String gwPartialName, int userId, String appId, int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd(endPartialName, userId, appId, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}

	}
	
	private static MyFogDevice addGw(String id, int parentId){
		MyFogDevice dept = createFogDevice("d-"+id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
		for(int i=0;i<numOfEndDevPerGateway;i++){
			String mobileId = id+"-"+i;
			MyFogDevice mobile = addMobile(mobileId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
			
			mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
		}
		return dept;
	}
	
	private static MyFogDevice addMobile(String id, int parentId){
		MyFogDevice mobile = createFogDevice("m-"+id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.setParentId(parentId);
		mobiles.add(mobile);
		/*Sensor eegSensor = new Sensor("s-"+id, "EEG", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
		sensors.add(eegSensor);
		Actuator display = new Actuator("a-"+id, userId, appId, "DISPLAY");
		actuators.add(display);
		eegSensor.setGatewayDeviceId(mobile.getId());
		eegSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
		display.setGatewayDeviceId(mobile.getId());
		display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
*/		return mobile;
	}
	

	private static MyFogDevice addEnd(String endPartialName, int userId, String appId, int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 3200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
		end.setParentId(parentId);
		idOfEndDevices.add(end.getId());
		MySensor sensor = new MySensor("s-" + endPartialName, "IoTSensor_1", userId, appId,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor);
		MyActuator actuator = new MyActuator("a-" + endPartialName, userId, appId, "IoTActuator_1");
		actuators.add(actuator);
		sensor.setGatewayDeviceId(end.getId());
		sensor.setLatency(6.0); // latency of connection between EEG sensors and
								// the parent Smartphone is 6 ms
		actuator.setGatewayDeviceId(end.getId());
		actuator.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		return end;
	}

	private static MyFogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level,
			double ratePerMips, double busyPower, double idlePower) {
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage,
				peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost,
				costPerMem, costPerStorage, costPerBw);

		MyFogDevice fogdevice = null;
		try {
			fogdevice = new MyFogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
					10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		fogdevice.setMips((int) mips);
		return fogdevice;
	}

	@SuppressWarnings({ "serial" })
	private static MyApplication createApplication0(String appId, int userId) {

		MyApplication application = MyApplication.createApplication(appId, userId);
		application.addAppModule("moduleA_1", 30, 1000, 1000, 100);
		application.addAppModule("moduleB_1", 150, 2000, 4000, 400);
		application.addAppModule("moduleC_1", 200, 1000, 4000, 100);
		application.addAppModule("moduleD_1", 20, 2000, 12000, 100);

		// modulesToPlace.add("moduleA_1");
		// modulesToPlace.add("moduleB_1");
		// modulesToPlace.add("moduleC_1");
		// modulesToPlace.add("moduleD_1");

		application.addAppEdge("IoTSensor_1", "moduleA_1", 400, 500, "IoTSensor_1", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("moduleA_1", "moduleB_1", 1000, 1000, "ProcessedData-1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleB_1", "moduleC_1", 1000, 1000, "ProcessedData-2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleC_1", "moduleD_1", 1500, 1000, "ProcessedData-3", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleD_1", "moduleA_1", 1000, 600, "ProcessedData-4", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("moduleA_1", "IoTActuator_1", 300, 100, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("moduleA_1", "IoTSensor_1", "ProcessedData-1", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleB_1", "ProcessedData-1", "ProcessedData-2", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleC_1", "ProcessedData-2", "ProcessedData-3", new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleD_1", "ProcessedData-3", "ProcessedData-4", new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleA_1", "ProcessedData-4", "OutputData", new FractionalSelectivity(1.0));

		for (int id : idOfEndDevices) {
			Map<String, Double> moduleDeadline = new HashMap<String, Double>();
			moduleDeadline.put("moduleB_1", getvalue(3.00, 5.00));
			Map<String, Integer> moduleAddMips = new HashMap<String, Integer>();
			moduleAddMips.put("moduleB_1", getvalue(0, 500));
			deadlineInfo.put(id, moduleDeadline);
			additionalMipsInfo.put(id, moduleAddMips);

		}

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("IoTSensor_1");
				add("moduleA_1");
				add("moduleB_1");
				add("moduleC_1");
				add("moduleD_1");
				add("moduleA_1");
				add("IoTActuator_1");
			}
		});

		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				// add(loop2);
			}
		};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo);
		application.setAdditionalMipsInfo(additionalMipsInfo);

		return application;
	}
}