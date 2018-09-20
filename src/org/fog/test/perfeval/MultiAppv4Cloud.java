package org.fog.test.perfeval;

// À partir deste ponto o ambiente executa com 04 aplicações 

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
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.entities.Sensor;
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
import org.fog.utils.distribution.Distribution;

public class MultiAppv4Cloud {
	static List<MyFogDevice> fogDevices = new ArrayList<MyFogDevice>();
	static Map<Integer, MyFogDevice> deviceById = new HashMap<Integer, MyFogDevice>();
	static List<MySensor> sensors = new ArrayList<MySensor>();
	static List<MyActuator> actuators = new ArrayList<MyActuator>();
	static List<Integer> idOfEndDevices = new ArrayList<Integer>();
	static Map<Integer, Map<String, Double>> deadlineInfo = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String, Double>> deadlineInfo1 = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Double>> deadlineInfo2 = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Double>> deadlineInfo3 = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Double>> deadlineInfo4 = new HashMap<Integer, Map<String, Double>>();

	static Map<Integer, Map<String, Integer>> additionalMipsInfo1 = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo2 = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo3 = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo4 = new HashMap<Integer, Map<String, Integer>>();

	static List<MyFogDevice> mobiles = new ArrayList<MyFogDevice>();

	static boolean CLOUD = false;

	static int numOfGateways = 2;
	static int numOfEndDevPerGateway = 4;
	static double sensingInterval = 5;
	static double EEG_TRANSMISSION_TIME = 5.1;

	public static void main(String[] args) {

		Log.printLine("Starting TestApplication...");

		try {
			Log.disable();
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
			CloudSim.init(num_user, calendar, trace_flag);

			String appId1 = "app1";
			String appId2 = "app2";
			String appId3 = "app3";
			String appId4 = "app4";

			FogBroker broker1 = new FogBroker("broker1");
			FogBroker broker2 = new FogBroker("broker2");
			FogBroker broker3 = new FogBroker("broker3");
			FogBroker broker4 = new FogBroker("broker4");


			createFogDevices(broker1.getId(), appId1, broker2.getId(), appId2, broker3.getId(), appId3, broker4.getId(), appId4);
			// createFogDevices2(broker2.getId(), appId2);
			// createFogDevices2(broker1.getId(), appId1);

			MyApplication application1 = createApplication0(appId1, broker1.getId());
			MyApplication application2 = createApplication1(appId2, broker2.getId());
			MyApplication application3 = createApplication2(appId3, broker3.getId());
			MyApplication application4 = createApplication3(appId4, broker4.getId());

			application1.setUserId(broker1.getId());
			application2.setUserId(broker2.getId());
			application3.setUserId(broker3.getId());
			application4.setUserId(broker4.getId());

			ModuleMapping moduleMapping1 = ModuleMapping.createModuleMapping();
			ModuleMapping moduleMapping2 = ModuleMapping.createModuleMapping();
			ModuleMapping moduleMapping3 = ModuleMapping.createModuleMapping();
			ModuleMapping moduleMapping4 = ModuleMapping.createModuleMapping();

			moduleMapping1.addModuleToDevice("moduleD_1", "cloud");
			moduleMapping1.addModuleToDevice("moduleC_1", "cloud");
			moduleMapping2.addModuleToDevice("moduleD_2", "cloud");
			moduleMapping2.addModuleToDevice("moduleC_2", "cloud");
			moduleMapping3.addModuleToDevice("moduleD_3", "cloud");
			moduleMapping3.addModuleToDevice("moduleC_3", "cloud");
			moduleMapping4.addModuleToDevice("moduleD_4", "cloud");
			moduleMapping4.addModuleToDevice("moduleC_4", "cloud");

			for (int i = 0; i < idOfEndDevices.size(); i++) {
				MyFogDevice fogDevice = deviceById.get(idOfEndDevices.get(i));
				moduleMapping1.addModuleToDevice("moduleA_1", fogDevice.getName());
				moduleMapping2.addModuleToDevice("moduleA_2", fogDevice.getName());
				moduleMapping3.addModuleToDevice("moduleA_3", fogDevice.getName());
				moduleMapping4.addModuleToDevice("moduleA_4", fogDevice.getName());

			}

			MyController controller = new MyController("master-controller", fogDevices, sensors, actuators);

			controller.submitApplication(application1, 0,
					new MyModulePlacement(fogDevices, sensors, actuators, application1, moduleMapping1));
			controller.submitApplication(application2, 0,
					new MyModulePlacement(fogDevices, sensors, actuators, application2, moduleMapping2));
			controller.submitApplication(application3, 0,
					new MyModulePlacement(fogDevices, sensors, actuators, application3, moduleMapping3));
			controller.submitApplication(application4, 0,
					new MyModulePlacement(fogDevices, sensors, actuators, application4, moduleMapping4));


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

	private static void createFogDevices2(int userId, String appId) {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw2(i + "", userId, appId, cloud.getId());
		}

	}

	private static void createFogDevices(int userId, String appId, int userId2, String appId2) {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw(i + "", userId, appId, userId2, appId2, cloud.getId());
		}

	}

	private static void createFogDevices(int userId, String appId, int userId2, String appId2, int userId3,
			String appId3) {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw(i + "", userId, appId, userId2, appId2, userId3, appId3, cloud.getId());
		}

	}
	
	private static void createFogDevices(int userId, String appId, int userId2, String appId2, int userId3,
			String appId3, int userId4,	String appId4) {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw(i + "", userId, appId, userId2, appId2, userId3, appId3,  userId4, appId4,  cloud.getId());
		}

	}

	private static void createFogDevices0() {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);

		for (int i = 0; i < numOfGateways; i++) {
			addGw0(i + "", cloud.getId());
		}

	}

	private static void createFogDevices() {
		MyFogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25); // creates
																												// the
																												// fog
																												// device
																												// Cloud
																												// at
																												// the
																												// apex
																												// of
																												// the
																												// hierarchy
																												// with
																												// level=0
		cloud.setParentId(-1);
		MyFogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates
																													// the
																													// fog
																													// device
																													// Proxy
																													// Server
																													// (level=1)
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the
											// Proxy Server
		proxy.setUplinkLatency(100); // latency of connection from Proxy Server
		deviceById.put(cloud.getId(), cloud);
		deviceById.put(proxy.getId(), proxy);
		// to the Cloud is 100 ms

		fogDevices.add(cloud);
		fogDevices.add(proxy);

		for (int i = 0; i < numOfGateways; i++) {
			addGw(i + "", proxy.getId()); // adding a fog device for every
											// Gateway in physical topology. The
											// parent of each gateway is the
											// Proxy Server
		}

	}

	private static void createEdgeDevices1(int userId, String appId) {
		for (MyFogDevice mobile : mobiles) {
			String id = mobile.getName();
			MySensor eegSensor = new MySensor("s-" + appId + "-" + id, "IoTSensor_1", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission
																			// time
																			// of
																			// EEG
																			// sensor
																			// follows
																			// a
																			// deterministic
																			// distribution
			sensors.add(eegSensor);
			MyActuator display = new MyActuator("a-" + appId + "-" + id, userId, appId, "IoTActuator_1");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG
										// sensors and the parent Smartphone is
										// 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display
										// actuator and the parent Smartphone is
										// 1 ms
		}
	}

	private static void createEdgeDevices2(int userId, String appId) {
		for (MyFogDevice mobile : mobiles) {
			String id = mobile.getName();
			MySensor eegSensor = new MySensor("s-" + appId + "-" + id, "IoTSensor_2", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission
																			// time
																			// of
																			// EEG
																			// sensor
																			// follows
																			// a
																			// deterministic
																			// distribution
			sensors.add(eegSensor);
			MyActuator display = new MyActuator("a-" + appId + "-" + id, userId, appId, "IoTActuator_2");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG
										// sensors and the parent Smartphone is
										// 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display
										// actuator and the parent Smartphone is
										// 1 ms
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

	private static void addGw2(String gwPartialName, int userId, String appId, int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd2(endPartialName, userId, appId, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}

	}

	private static void addGw(String gwPartialName, int userId, String appId, int userId2, String appId2,
			int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd(endPartialName, userId, appId, userId2, appId2, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}

	}

	private static void addGw(String gwPartialName, int userId, String appId, int userId2, String appId2, int userId3,
			String appId3, int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd(endPartialName, userId, appId, userId2, appId2, userId3, appId3, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}

	}
	

	private static void addGw(String gwPartialName, int userId, String appId, int userId2, String appId2, int userId3,
			String appId3, int userId4,	String appId4, int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd(endPartialName, userId, appId, userId2, appId2, userId3, appId3, userId4, appId4, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}

	}

	private static void addGw0(String gwPartialName, int parentId) {
		MyFogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(4);
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String endPartialName = gwPartialName + "-" + i;
			MyFogDevice end = addEnd0(endPartialName, gw.getId());
			end.setUplinkLatency(2);
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}
	}

	private static MyFogDevice addGw(String id, int parentId) {
		MyFogDevice dept = createFogDevice("d-" + id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(dept);
		deviceById.put(dept.getId(), dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(4); // latency of connection between gateways and
									// proxy server is 4 ms
		for (int i = 0; i < numOfEndDevPerGateway; i++) {
			String mobileId = id + "-" + i;
			MyFogDevice mobile = addMobile(mobileId, dept.getId()); // adding
																	// mobiles
																	// to the
																	// physical
																	// topology.
																	// Smartphones
																	// have been
																	// modeled
																	// as fog
																	// devices
																	// as well.

			mobile.setUplinkLatency(2); // latency of connection between the
										// smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
			deviceById.put(mobile.getId(), mobile);
		}
		return dept;
	}

	private static MyFogDevice addMobile(String id, int parentId) {
		MyFogDevice mobile = createFogDevice("m-" + id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.setParentId(parentId);
		mobiles.add(mobile);
		idOfEndDevices.add(mobile.getId());

		/*
		 * Sensor eegSensor = new Sensor("s-"+id, "EEG", userId, appId, new
		 * DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time
		 * of EEG sensor follows a deterministic distribution sensors.add(eegSensor);
		 * Actuator display = new Actuator("a-"+id, userId, appId, "DISPLAY");
		 * actuators.add(display); eegSensor.setGatewayDeviceId(mobile.getId());
		 * eegSensor.setLatency(6.0); // latency of connection between EEG sensors and
		 * the parent Smartphone is 6 ms display.setGatewayDeviceId(mobile.getId());
		 * display.setLatency(1.0); // latency of connection between Display actuator
		 * and the parent Smartphone is 1 ms
		 */ return mobile;
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

	private static MyFogDevice addEnd2(String endPartialName, int userId, String appId, int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 3200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
		end.setParentId(parentId);
		idOfEndDevices.add(end.getId());
		MySensor sensor = new MySensor("s-" + endPartialName, "IoTSensor_2", userId, appId,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor);
		MyActuator actuator = new MyActuator("a-" + endPartialName, userId, appId, "IoTActuator_2");
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

	private static MyFogDevice addEnd(String endPartialName, int userId, String appId, int userId2, String appId2,
			int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 13200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
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

		MySensor sensor2 = new MySensor("s-" + endPartialName, "IoTSensor_2", userId2, appId2,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor2);
		MyActuator actuator2 = new MyActuator("a-" + endPartialName, userId2, appId2, "IoTActuator_2");
		actuators.add(actuator2);
		sensor2.setGatewayDeviceId(end.getId());
		sensor2.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator2.setGatewayDeviceId(end.getId());
		actuator2.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		return end;
	}

	private static MyFogDevice addEnd(String endPartialName, int userId, String appId, int userId2, String appId2,
			int userId3, String appId3, int userId4, String appId4, int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 13200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
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

		MySensor sensor2 = new MySensor("s-" + endPartialName, "IoTSensor_2", userId2, appId2,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor2);
		MyActuator actuator2 = new MyActuator("a-" + endPartialName, userId2, appId2, "IoTActuator_2");
		actuators.add(actuator2);
		sensor2.setGatewayDeviceId(end.getId());
		sensor2.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator2.setGatewayDeviceId(end.getId());
		actuator2.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		MySensor sensor3 = new MySensor("s-" + endPartialName, "IoTSensor_3", userId3, appId3,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor3);
		MyActuator actuator3 = new MyActuator("a-" + endPartialName, userId3, appId3, "IoTActuator_3");
		actuators.add(actuator3);
		sensor3.setGatewayDeviceId(end.getId());
		sensor3.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator3.setGatewayDeviceId(end.getId());
		actuator3.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		
		MySensor sensor4 = new MySensor("s-" + endPartialName, "IoTSensor_4", userId4, appId4,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor4);
		MyActuator actuator4 = new MyActuator("a-" + endPartialName, userId4, appId4, "IoTActuator_4");
		actuators.add(actuator4);
		sensor4.setGatewayDeviceId(end.getId());
		sensor4.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator4.setGatewayDeviceId(end.getId());
		actuator4.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		
		

		return end;
	}
	
	private static MyFogDevice addEnd(String endPartialName, int userId, String appId, int userId2, String appId2,
			int userId3, String appId3, int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 13200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
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

		MySensor sensor2 = new MySensor("s-" + endPartialName, "IoTSensor_2", userId2, appId2,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor2);
		MyActuator actuator2 = new MyActuator("a-" + endPartialName, userId2, appId2, "IoTActuator_2");
		actuators.add(actuator2);
		sensor2.setGatewayDeviceId(end.getId());
		sensor2.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator2.setGatewayDeviceId(end.getId());
		actuator2.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		MySensor sensor3 = new MySensor("s-" + endPartialName, "IoTSensor_3", userId3, appId3,
				new DeterministicDistribution(sensingInterval)); // inter-transmission
																	// time of
																	// EEG
																	// sensor
																	// follows a
																	// deterministic
																	// distribution
		sensors.add(sensor3);
		MyActuator actuator3 = new MyActuator("a-" + endPartialName, userId3, appId3, "IoTActuator_3");
		actuators.add(actuator3);
		sensor3.setGatewayDeviceId(end.getId());
		sensor3.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
		actuator3.setGatewayDeviceId(end.getId());
		actuator3.setLatency(1.0); // latency of connection between Display
									// actuator and the parent Smartphone is 1
									// ms
		
		
		return end;
	}

	private static MyFogDevice addEnd0(String endPartialName, int parentId) {
		MyFogDevice end = createFogDevice("e-" + endPartialName, 3200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
		end.setParentId(parentId);
		idOfEndDevices.add(end.getId()); // actuator and the parent Smartphone
											// is 1
		// ms
		return end;
	}

	private static void distributeSensor1(int userId, String appId) {

		for (int i = 0; i < idOfEndDevices.size(); i++) {

			MySensor sensor = new MySensor("s-" + i, "IoTSensor_1", userId, appId,
					new DeterministicDistribution(sensingInterval)); // inter-transmission
																		// time
																		// of
																		// EEG
																		// sensor
																		// follows
																		// a
																		// deterministic
																		// distribution
			sensors.add(sensor);
			MyActuator actuator = new MyActuator("a-" + i, userId, appId, "IoTActuator_1");
			actuators.add(actuator);
			sensor.setGatewayDeviceId(idOfEndDevices.get(i));
			sensor.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
			actuator.setGatewayDeviceId(idOfEndDevices.get(i));
			actuator.setLatency(1.0); // latency of connection between Display
		}
	}

	private static void distributeSensor2(int userId, String appId) {

		for (int i = 0; i < idOfEndDevices.size(); i++) {

			MySensor sensor = new MySensor("s2-" + i, "IoTSensor_2", userId, appId,
					new DeterministicDistribution(sensingInterval)); // inter-transmission
																		// time
																		// of
																		// EEG
																		// sensor
																		// follows
																		// a
																		// deterministic
																		// distribution
			sensors.add(sensor);
			MyActuator actuator = new MyActuator("a2-" + i, userId, appId, "IoTActuator_2");
			actuators.add(actuator);
			sensor.setGatewayDeviceId(idOfEndDevices.get(i));
			sensor.setLatency(6.0); // latency of connection between EEG sensors
									// and
									// the parent Smartphone is 6 ms
			actuator.setGatewayDeviceId(idOfEndDevices.get(i));
			actuator.setLatency(1.0); // latency of connection between Display
		}
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
		application.addAppModule("moduleA_1", 10, 1000, 1000, 100);
		application.addAppModule("moduleB_1", 10, 600, 4000, 100);
		application.addAppModule("moduleC_1", 10, 600, 4000, 100);
		application.addAppModule("moduleD_1", 10, 50, 12000, 100);

		// modulesToPlace.add("moduleA_1");
		// modulesToPlace.add("moduleB_1");
		// modulesToPlace.add("moduleC_1");
		// modulesToPlace.add("moduleD_1");

		application.addAppEdge("IoTSensor_1", "moduleA_1", 100, 500, "IoTSensor_1", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("moduleA_1", "moduleB_1", 100, 1000, "ProcessedData-1_1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleB_1", "moduleC_1", 100, 1000, "ProcessedData-2_1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleC_1", "moduleD_1", 100, 1000, "ProcessedData-3_1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleD_1", "moduleA_1", 100, 1000, "ProcessedData-4_1", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("moduleA_1", "IoTActuator_1", 100, 1000, "OutputData_1", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("moduleA_1", "IoTSensor_1", "ProcessedData-1_1", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleB_1", "ProcessedData-1_1", "ProcessedData-2_1",
				new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleC_1", "ProcessedData-2_1", "ProcessedData-3_1",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleD_1", "ProcessedData-3_1", "ProcessedData-4_1",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleA_1", "ProcessedData-4_1", "OutputData_1", new FractionalSelectivity(1.0));

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

	@SuppressWarnings({ "serial" })
	private static MyApplication createApplication1(String appId, int userId) {

		MyApplication application = MyApplication.createApplication(appId, userId);
		application.addAppModule("moduleA_2", 10, 1000, 1000, 100);
		application.addAppModule("moduleB_2", 10, 600, 4000, 100);
		application.addAppModule("moduleC_2", 10, 600, 4000, 100);
		application.addAppModule("moduleD_2", 10, 50, 12000, 100);

		// modulesToPlace.add("moduleA_2");
		// modulesToPlace.add("moduleB_2");
		// modulesToPlace.add("moduleC_2");
		// modulesToPlace.add("moduleD_2");

		application.addAppEdge("IoTSensor_2", "moduleA_2", 100, 500, "IoTSensor_2", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("moduleA_2", "moduleB_2", 100, 1000, "ProcessedData-1_2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleB_2", "moduleC_2", 100, 1000, "ProcessedData-2_2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleC_2", "moduleD_2", 100, 1000, "ProcessedData-3_2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleD_2", "moduleA_2", 100, 1000, "ProcessedData-4_2", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("moduleA_2", "IoTActuator_2", 100, 1000, "OutputData_2", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("moduleA_2", "IoTSensor_2", "ProcessedData-1_2", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleB_2", "ProcessedData-1_2", "ProcessedData-2_2",
				new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleC_2", "ProcessedData-2_2", "ProcessedData-3_2",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleD_2", "ProcessedData-3_2", "ProcessedData-4_2",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleA_2", "ProcessedData-4_2", "OutputData_2", new FractionalSelectivity(1.0));

		for (int id : idOfEndDevices) {
			Map<String, Double> moduleDeadline = new HashMap<String, Double>();
			moduleDeadline.put("moduleB_2", getvalue(3.00, 5.00));
			Map<String, Integer> moduleAddMips = new HashMap<String, Integer>();
			moduleAddMips.put("moduleB_2", getvalue(0, 500));
			deadlineInfo1.put(id, moduleDeadline);
			additionalMipsInfo1.put(id, moduleAddMips);
		}

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("IoTSensor_2");
				add("moduleA_2");
				add("moduleB_2");
				add("moduleC_2");
				add("moduleD_2");
				add("moduleA_2");
				add("IoTActuator_2");
			}
		});

		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				// add(loop2);
			}
		};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo1);
		application.setAdditionalMipsInfo(additionalMipsInfo1);

		return application;
	}

	@SuppressWarnings({ "serial" })
	private static MyApplication createApplication2(String appId, int userId) {

		MyApplication application = MyApplication.createApplication(appId, userId);
		application.addAppModule("moduleA_3", 10, 1000, 1000, 100);
		application.addAppModule("moduleB_3", 10, 600, 4000, 100);
		application.addAppModule("moduleC_3", 10, 600, 4000, 100);
		application.addAppModule("moduleD_3", 10, 50, 12000, 100);

		// modulesToPlace.add("moduleA_3");
		// modulesToPlace.add("moduleB_3");
		// modulesToPlace.add("moduleC_3");
		// modulesToPlace.add("moduleD_3");

		application.addAppEdge("IoTSensor_3", "moduleA_3", 100, 500, "IoTSensor_3", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("moduleA_3", "moduleB_3", 100, 1000, "ProcessedData-1_3", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleB_3", "moduleC_3", 100, 1000, "ProcessedData-2_3", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleC_3", "moduleD_3", 100, 1000, "ProcessedData-3_3", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleD_3", "moduleA_3", 100, 1000, "ProcessedData-4_3", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("moduleA_3", "IoTActuator_3", 100, 1000, "OutputData_3", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("moduleA_3", "IoTSensor_3", "ProcessedData-1_3", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleB_3", "ProcessedData-1_3", "ProcessedData-2_3",
				new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleC_3", "ProcessedData-2_3", "ProcessedData-3_3",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleD_3", "ProcessedData-3_3", "ProcessedData-4_3",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleA_3", "ProcessedData-4_3", "OutputData_3", new FractionalSelectivity(1.0));

		for (int id : idOfEndDevices) {
			Map<String, Double> moduleDeadline = new HashMap<String, Double>();
			moduleDeadline.put("moduleB_3", getvalue(3.00, 5.00));
			Map<String, Integer> moduleAddMips = new HashMap<String, Integer>();
			moduleAddMips.put("moduleB_3", getvalue(0, 500));
			deadlineInfo2.put(id, moduleDeadline);
			additionalMipsInfo2.put(id, moduleAddMips);
		}

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("IoTSensor_3");
				add("moduleA_3");
				add("moduleB_3");
				add("moduleC_3");
				add("moduleD_3");
				add("moduleA_3");
				add("IoTActuator_3");
			}
		});

		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				// add(loop2);
			}
		};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo2);
		application.setAdditionalMipsInfo(additionalMipsInfo2);

		return application;
	}

	@SuppressWarnings({ "serial" })
	private static MyApplication createApplication3(String appId, int userId) {

		MyApplication application = MyApplication.createApplication(appId, userId);
		application.addAppModule("moduleA_4", 10, 1000, 1000, 100);
		application.addAppModule("moduleB_4", 10, 600, 4000, 100);
		application.addAppModule("moduleC_4", 10, 600, 4000, 100);
		application.addAppModule("moduleD_4", 10, 50, 12000, 100);

		// modulesToPlace.add("moduleA_4");
		// modulesToPlace.add("moduleB_4");
		// modulesToPlace.add("moduleC_4");
		// modulesToPlace.add("moduleD_4");

		application.addAppEdge("IoTSensor_4", "moduleA_4", 100, 500, "IoTSensor_4", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("moduleA_4", "moduleB_4", 100, 1000, "ProcessedData-1_4", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleB_4", "moduleC_4", 100, 1000, "ProcessedData-2_4", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleC_4", "moduleD_4", 100, 1000, "ProcessedData-3_4", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("moduleD_4", "moduleA_4", 100, 1000, "ProcessedData-4_4", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("moduleA_4", "IoTActuator_4", 100, 1000, "OutputData_4", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("moduleA_4", "IoTSensor_4", "ProcessedData-1_4", new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleB_4", "ProcessedData-1_4", "ProcessedData-2_4",
				new FractionalSelectivity(0.9));
		application.addTupleMapping("moduleC_4", "ProcessedData-2_4", "ProcessedData-3_4",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleD_4", "ProcessedData-3_4", "ProcessedData-4_4",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("moduleA_4", "ProcessedData-4_4", "OutputData_4", new FractionalSelectivity(1.0));

		for (int id : idOfEndDevices) {
			Map<String, Double> moduleDeadline = new HashMap<String, Double>();
			moduleDeadline.put("moduleB_4", getvalue(3.00, 5.00));
			Map<String, Integer> moduleAddMips = new HashMap<String, Integer>();
			moduleAddMips.put("moduleB_4", getvalue(0, 500));
			deadlineInfo4.put(id, moduleDeadline);
			additionalMipsInfo4.put(id, moduleAddMips);
		}

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("IoTSensor_4");
				add("moduleA_4");
				add("moduleB_4");
				add("moduleC_4");
				add("moduleD_4");
				add("moduleA_4");
				add("IoTActuator_4");
			}
		});

		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				// add(loop2);
			}
		};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo4);
		application.setAdditionalMipsInfo(additionalMipsInfo4);

		return application;
	}
}