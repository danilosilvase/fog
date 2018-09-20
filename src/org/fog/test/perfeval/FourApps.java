package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

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
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for case study 1 - EEG Beam Tractor Game
 * 
 * @author Harshit Gupta
 *
 */
public class FourApps {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<FogDevice> mobiles = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();

	// ambiente ok
	//
	static int numOfDepts = 2;
	static int numOfMobilesPerDept = 4;
	static double EEG_TRANSMISSION_TIME = 5.1;
	// static double EEG_TRANSMISSION_TIME = 10;

	public static void main(String[] args) {

		Log.printLine("Starting TwoApps...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId0 = "app_0";
			String appId1 = "app_1";
			String appId2 = "app_2";
			String appId3 = "app_3";

			FogBroker broker0 = new FogBroker("broker_0");
			FogBroker broker1 = new FogBroker("broker_1");
			FogBroker broker2 = new FogBroker("broker_2");
			FogBroker broker3 = new FogBroker("broker_3");

			Application application0 = createApplication0(appId0, broker0.getId());
			Application application1 = createApplication1(appId1, broker1.getId());
			Application application2 = createApplication2(appId2, broker2.getId());
			Application application3 = createApplication3(appId3, broker3.getId());
			application0.setUserId(broker0.getId());
			application1.setUserId(broker1.getId());
			application2.setUserId(broker2.getId());
			application3.setUserId(broker3.getId());

			createFogDevices();

			createEdgeDevices0(broker0.getId(), appId0);
			createEdgeDevices1(broker1.getId(), appId1);
			createEdgeDevices2(broker2.getId(), appId2);
			createEdgeDevices3(broker3.getId(), appId3);

			ModuleMapping moduleMapping_0 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			ModuleMapping moduleMapping_1 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			ModuleMapping moduleMapping_2 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			ModuleMapping moduleMapping_3 = ModuleMapping.createModuleMapping(); // initializing a module mapping

			moduleMapping_0.addModuleToDevice("connector", "cloud"); // fixing all instances of the Connector module to
																		// the Cloud
			moduleMapping_0.addModuleToDevice("concentration_calculator", "cloud"); // fixing all instances of the
																					// Concentration Calculator module
																					// to the Cloud
			moduleMapping_1.addModuleToDevice("connector_1", "cloud"); // fixing all instances of the Connector module
																		// to the Cloud
			moduleMapping_1.addModuleToDevice("concentration_calculator_1", "cloud"); // fixing all instances of the
																						// Concentration Calculator
																						// module to the Cloud
			moduleMapping_2.addModuleToDevice("connector_2", "cloud"); // fixing all instances of the Connector module
																		// to the Cloud
			moduleMapping_2.addModuleToDevice("concentration_calculator_2", "cloud"); // fixing all instances of the
																						// Concentration Calculator
																						// module to the Cloud

			moduleMapping_3.addModuleToDevice("connector_3", "cloud"); // fixing all instances of the Connector module
			// to the Cloud
			moduleMapping_3.addModuleToDevice("concentration_calculator_3", "cloud"); // fixing all instances of the
			// Concentration Calculator
			// module to the Cloud

			for (FogDevice device : fogDevices) {
				if (device.getName().startsWith("m")) {
					moduleMapping_0.addModuleToDevice("client", device.getName()); // fixing all instances of the Client
																					// module to the Smartphones
					moduleMapping_1.addModuleToDevice("client_1", device.getName()); // fixing all instances of the
																						// Client module to the
																						// Smartphones
					moduleMapping_2.addModuleToDevice("client_2", device.getName()); // fixing all instances of the
																						// Client module to the
																						// Smartphones
					moduleMapping_3.addModuleToDevice("client_3", device.getName());
				}
			}

			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

			controller.submitApplication(application0, 0,
					new ModulePlacementMapping(fogDevices, application0, moduleMapping_0));
			controller.submitApplication(application1, 0,
					new ModulePlacementMapping(fogDevices, application1, moduleMapping_1));
			controller.submitApplication(application2, 0,
					new ModulePlacementMapping(fogDevices, application2, moduleMapping_2));
			controller.submitApplication(application3, 0,
					new ModulePlacementMapping(fogDevices, application3, moduleMapping_3));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("FourApp finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	private static void createEdgeDevices0(int userId, String appId) {
		for (FogDevice mobile : mobiles) {
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-" + appId + "-" + id, "EEG", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor
																			// follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-" + appId + "-" + id, userId, appId, "DISPLAY");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display actuator and the parent Smartphone is 1
										// ms
		}
	}

	private static void createEdgeDevices1(int userId, String appId) {
		for (FogDevice mobile : mobiles) {
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-" + appId + "-" + id, "EEG_1", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor
																			// follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-" + appId + "-" + id, userId, appId, "DISPLAY_1");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display actuator and the parent Smartphone is 1
										// ms
		}
	}

	private static void createEdgeDevices2(int userId, String appId) {
		for (FogDevice mobile : mobiles) {
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-" + appId + "-" + id, "EEG_2", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor
																			// follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-" + appId + "-" + id, userId, appId, "DISPLAY_2");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display actuator and the parent Smartphone is 1
										// ms
		}
	}
	
	
	private static void createEdgeDevices3(int userId, String appId) {
		for (FogDevice mobile : mobiles) {
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-" + appId + "-" + id, "EEG_3", userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor
																			// follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-" + appId + "-" + id, userId, appId, "DISPLAY_3");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0); // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0); // latency of connection between Display actuator and the parent Smartphone is 1
										// ms
		}
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * 
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25); // creates
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
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates
																												// the
																												// fog
																												// device
																												// Proxy
																												// Server
																												// (level=1)
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
		proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms

		fogDevices.add(cloud);
		fogDevices.add(proxy);

		for (int i = 0; i < numOfDepts; i++) {
			addGw(i + "", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of
											// each gateway is the Proxy Server
		}

	}

	private static FogDevice addGw(String id, int parentId) {
		FogDevice dept = createFogDevice("d-" + id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
		for (int i = 0; i < numOfMobilesPerDept; i++) {
			String mobileId = id + "-" + i;
			FogDevice mobile = addMobile(mobileId, dept.getId()); // adding mobiles to the physical topology.
																	// Smartphones have been modeled as fog devices as
																	// well.

			mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
		}
		return dept;
	}

	private static FogDevice addMobile(String id, int parentId) {
		FogDevice mobile = createFogDevice("m-" + id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.setParentId(parentId);
		mobiles.add(mobile);
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

	/**
	 * Creates a vanilla fog device
	 * 
	 * @param nodeName
	 *            name of the device to be used in simulation
	 * @param mips
	 *            MIPS
	 * @param ram
	 *            RAM
	 * @param upBw
	 *            uplink bandwidth
	 * @param downBw
	 *            downlink bandwidth
	 * @param level
	 *            hierarchy level of the device
	 * @param ratePerMips
	 *            cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level,
			double ratePerMips, double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage,
				peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
		// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost,
				costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
					10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Function to create the EEG Tractor Beam game application in the DDF model.
	 * 
	 * @param appId
	 *            unique identifier of the application
	 * @param userId
	 *            identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({ "serial" })
	private static Application createApplication0(String appId, int userId) {

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)

		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("client", 10); // adding module Client to the application model
		application.addAppModule("concentration_calculator", 10); // adding module Concentration Calculator to the
																	// application model
		application.addAppModule("connector", 10); // adding module Connector to the application model

		/*
		 * Connecting the application modules (vertices) in the application model
		 * (directed graph) with edges
		 */
		if (EEG_TRANSMISSION_TIME == 10)
			application.addAppEdge("EEG", "client", 2000, 500, "EEG", Tuple.UP, AppEdge.SENSOR); // adding edge from EEG
																									// (sensor) to
																									// Client module
																									// carrying tuples
																									// of type EEG
		else
			application.addAppEdge("EEG", "client", 3000, 500, "EEG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "concentration_calculator", 3500, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE); // adding
																														// edge
																														// from
																														// Client
																														// to
																														// Concentration
																														// Calculator
																														// module
																														// carrying
																														// tuples
																														// of
																														// type
																														// _SENSOR
		application.addAppEdge("concentration_calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", Tuple.UP,
				AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to
									// Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator", "client", 14, 500, "CONCENTRATION", Tuple.DOWN,
				AppEdge.MODULE); // adding edge from Concentration Calculator to Client module carrying tuples of
									// type CONCENTRATION
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE); // adding
																														// periodic
																														// edge
																														// (period=1000ms)
																														// from
																														// Connector
																														// to
																														// Client
																														// module
																														// carrying
																														// tuples
																														// of
																														// type
																														// GLOBAL_GAME_STATE
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																													// edge
																													// from
																													// Client
																													// module
																													// to
																													// Display
																													// (actuator)
																													// carrying
																													// tuples
																													// of
																													// type
																													// SELF_STATE_UPDATE
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																														// edge
																														// from
																														// Client
																														// module
																														// to
																														// Display
																														// (actuator)
																														// carrying
																														// tuples
																														// of
																														// type
																														// GLOBAL_STATE_UPDATE

		/*
		 * Defining the input-output relationships (represented by selectivity) of the
		 * application modules.
		 */
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9)); // 0.9 tuples of type
																									// _SENSOR are
																									// emitted by Client
																									// module per
																									// incoming tuple of
																									// type EEG
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0
																														// tuples
																														// of
																														// type
																														// SELF_STATE_UPDATE
																														// are
																														// emitted
																														// by
																														// Client
																														// module
																														// per
																														// incoming
																														// tuple
																														// of
																														// type
																														// CONCENTRATION
		application.addTupleMapping("concentration_calculator", "_SENSOR", "CONCENTRATION",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration
													// Calculator module per incoming tuple of type _SENSOR
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type GLOBAL_GAME_STATE

		/*
		 * Defining application loops to monitor the latency of. Here, we add only one
		 * loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator ->
		 * Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("EEG");
				add("client");
				add("concentration_calculator");
				add("client");
				add("DISPLAY");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;
	}

	@SuppressWarnings({ "serial" })
	private static Application createApplication1(String appId, int userId) {

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)

		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("client_1", 10); // adding module Client to the application model
		application.addAppModule("concentration_calculator_1", 10); // adding module Concentration Calculator to the
																	// application model
		application.addAppModule("connector_1", 10); // adding module Connector to the application model

		/*
		 * Connecting the application modules (vertices) in the application model
		 * (directed graph) with edges
		 */
		if (EEG_TRANSMISSION_TIME == 10)
			application.addAppEdge("EEG_1", "client_1", 2000, 500, "EEG_1", Tuple.UP, AppEdge.SENSOR); // adding edge
																										// from EEG
																										// (sensor) to
																										// Client module
																										// carrying
																										// tuples of
																										// type EEG
		else
			application.addAppEdge("EEG_1", "client_1", 3000, 500, "EEG_1", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client_1", "concentration_calculator_1", 3500, 500, "_SENSOR_1", Tuple.UP,
				AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of
									// type _SENSOR
		application.addAppEdge("concentration_calculator_1", "connector_1", 100, 1000, 1000, "PLAYER_GAME_STATE_1",
				Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to
											// Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator_1", "client_1", 14, 500, "CONCENTRATION_1", Tuple.DOWN,
				AppEdge.MODULE); // adding edge from Concentration Calculator to Client module carrying tuples of
									// type CONCENTRATION
		application.addAppEdge("connector_1", "client_1", 100, 28, 1000, "GLOBAL_GAME_STATE_1", Tuple.DOWN,
				AppEdge.MODULE); // adding periodic edge (period=1000ms) from Connector to Client module carrying
									// tuples of type GLOBAL_GAME_STATE
		application.addAppEdge("client_1", "DISPLAY_1", 1000, 500, "SELF_STATE_UPDATE_1", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																															// edge
																															// from
																															// Client
																															// module
																															// to
																															// Display
																															// (actuator)
																															// carrying
																															// tuples
																															// of
																															// type
																															// SELF_STATE_UPDATE
		application.addAppEdge("client_1", "DISPLAY_1", 1000, 500, "GLOBAL_STATE_UPDATE_1", Tuple.DOWN,
				AppEdge.ACTUATOR); // adding edge from Client module to Display (actuator) carrying tuples of type
									// GLOBAL_STATE_UPDATE

		/*
		 * Defining the input-output relationships (represented by selectivity) of the
		 * application modules.
		 */
		application.addTupleMapping("client_1", "EEG_1", "_SENSOR_1", new FractionalSelectivity(0.9)); // 0.9 tuples of
																										// type _SENSOR
																										// are emitted
																										// by Client
																										// module per
																										// incoming
																										// tuple of type
																										// EEG
		application.addTupleMapping("client_1", "CONCENTRATION_1", "SELF_STATE_UPDATE_1",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type CONCENTRATION
		application.addTupleMapping("concentration_calculator_1", "_SENSOR_1", "CONCENTRATION_1",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration
													// Calculator module per incoming tuple of type _SENSOR
		application.addTupleMapping("client_1", "GLOBAL_GAME_STATE_1", "GLOBAL_STATE_UPDATE_1",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type GLOBAL_GAME_STATE

		/*
		 * Defining application loops to monitor the latency of. Here, we add only one
		 * loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator ->
		 * Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("EEG_1");
				add("client_1");
				add("concentration_calculator_1");
				add("client_1");
				add("DISPLAY_1");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;

	}

	private static Application createApplication2(String appId, int userId) {

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)

		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("client_2", 10); // adding module Client to the application model
		application.addAppModule("concentration_calculator_2", 10); // adding module Concentration Calculator to the
																	// application model
		application.addAppModule("connector_2", 10); // adding module Connector to the application model

		/*
		 * Connecting the application modules (vertices) in the application model
		 * (directed graph) with edges
		 */
		if (EEG_TRANSMISSION_TIME == 10)
			application.addAppEdge("EEG_2", "client_2", 2000, 500, "EEG_2", Tuple.UP, AppEdge.SENSOR); // adding edge
																										// from EEG
																										// (sensor) to
																										// Client module
																										// carrying
																										// tuples of
																										// type EEG
		else
			application.addAppEdge("EEG_2", "client_2", 3000, 500, "EEG_2", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client_2", "concentration_calculator_2", 3500, 500, "_SENSOR_2", Tuple.UP,
				AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of
									// type _SENSOR
		application.addAppEdge("concentration_calculator_2", "connector_2", 100, 1000, 1000, "PLAYER_GAME_STATE_2",
				Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to
											// Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator_2", "client_2", 14, 500, "CONCENTRATION_2", Tuple.DOWN,
				AppEdge.MODULE); // adding edge from Concentration Calculator to Client module carrying tuples of
									// type CONCENTRATION
		application.addAppEdge("connector_2", "client_2", 100, 28, 1000, "GLOBAL_GAME_STATE_2", Tuple.DOWN,
				AppEdge.MODULE); // adding periodic edge (period=1000ms) from Connector to Client module carrying
									// tuples of type GLOBAL_GAME_STATE
		application.addAppEdge("client_2", "DISPLAY_2", 1000, 500, "SELF_STATE_UPDATE_2", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																															// edge
																															// from
																															// Client
																															// module
																															// to
																															// Display
																															// (actuator)
																															// carrying
																															// tuples
																															// of
																															// type
																															// SELF_STATE_UPDATE
		application.addAppEdge("client_2", "DISPLAY_2", 1000, 500, "GLOBAL_STATE_UPDATE_2", Tuple.DOWN,
				AppEdge.ACTUATOR); // adding edge from Client module to Display (actuator) carrying tuples of type
									// GLOBAL_STATE_UPDATE

		/*
		 * Defining the input-output relationships (represented by selectivity) of the
		 * application modules.
		 */
		application.addTupleMapping("client_2", "EEG_2", "_SENSOR_2", new FractionalSelectivity(0.9)); // 0.9 tuples of
																										// type _SENSOR
																										// are emitted
																										// by Client
																										// module per
																										// incoming
																										// tuple of type
																										// EEG
		application.addTupleMapping("client_2", "CONCENTRATION_2", "SELF_STATE_UPDATE_2",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type CONCENTRATION
		application.addTupleMapping("concentration_calculator_2", "_SENSOR_2", "CONCENTRATION_2",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration
													// Calculator module per incoming tuple of type _SENSOR
		application.addTupleMapping("client_2", "GLOBAL_GAME_STATE_2", "GLOBAL_STATE_UPDATE_2",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type GLOBAL_GAME_STATE

		/*
		 * Defining application loops to monitor the latency of. Here, we add only one
		 * loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator ->
		 * Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("EEG_2");
				add("client_2");
				add("concentration_calculator_2");
				add("client_2");
				add("DISPLAY_2");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;

	}

	private static Application createApplication3(String appId, int userId) {

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)

		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("client_3", 10); // adding module Client to the application model
		application.addAppModule("concentration_calculator_3", 10); // adding module Concentration Calculator to the
																	// application model
		application.addAppModule("connector_3", 10); // adding module Connector to the application model

		/*
		 * Connecting the application modules (vertices) in the application model
		 * (directed graph) with edges
		 */
		if (EEG_TRANSMISSION_TIME == 10)
			application.addAppEdge("EEG_3", "client_3", 2000, 500, "EEG_3", Tuple.UP, AppEdge.SENSOR); // adding edge
																										// from EEG
																										// (sensor) to
																										// Client module
																										// carrying
																										// tuples of
																										// type EEG
		else
			application.addAppEdge("EEG_3", "client_3", 3000, 500, "EEG_3", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client_3", "concentration_calculator_3", 3500, 500, "_SENSOR_3", Tuple.UP,
				AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of
									// type _SENSOR
		application.addAppEdge("concentration_calculator_3", "connector_3", 100, 1000, 1000, "PLAYER_GAME_STATE_3",
				Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to
											// Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator_3", "client_3", 14, 500, "CONCENTRATION_3", Tuple.DOWN,
				AppEdge.MODULE); // adding edge from Concentration Calculator to Client module carrying tuples of
									// type CONCENTRATION
		application.addAppEdge("connector_3", "client_3", 100, 28, 1000, "GLOBAL_GAME_STATE_3", Tuple.DOWN,
				AppEdge.MODULE); // adding periodic edge (period=1000ms) from Connector to Client module carrying
									// tuples of type GLOBAL_GAME_STATE
		application.addAppEdge("client_3", "DISPLAY_3", 1000, 500, "SELF_STATE_UPDATE_3", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																															// edge
																															// from
																															// Client
																															// module
																															// to
																															// Display
																															// (actuator)
																															// carrying
																															// tuples
																															// of
																															// type
																															// SELF_STATE_UPDATE
		application.addAppEdge("client_3", "DISPLAY_3", 1000, 500, "GLOBAL_STATE_UPDATE_3", Tuple.DOWN,
				AppEdge.ACTUATOR); // adding edge from Client module to Display (actuator) carrying tuples of type
									// GLOBAL_STATE_UPDATE

		/*
		 * Defining the input-output relationships (represented by selectivity) of the
		 * application modules.
		 */
		application.addTupleMapping("client_3", "EEG_3", "_SENSOR_3", new FractionalSelectivity(0.9)); // 0.9 tuples of
																										// type _SENSOR
																										// are emitted
																										// by Client
																										// module per
																										// incoming
																										// tuple of type
																										// EEG
		application.addTupleMapping("client_3", "CONCENTRATION_3", "SELF_STATE_UPDATE_3",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type CONCENTRATION
		application.addTupleMapping("concentration_calculator_3", "_SENSOR_3", "CONCENTRATION_3",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration
													// Calculator module per incoming tuple of type _SENSOR
		application.addTupleMapping("client_3", "GLOBAL_GAME_STATE_3", "GLOBAL_STATE_UPDATE_3",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type GLOBAL_GAME_STATE

		/*
		 * Defining application loops to monitor the latency of. Here, we add only one
		 * loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator ->
		 * Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("EEG_3");
				add("client_3");
				add("concentration_calculator_3");
				add("client_3");
				add("DISPLAY_3");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;

	}
}