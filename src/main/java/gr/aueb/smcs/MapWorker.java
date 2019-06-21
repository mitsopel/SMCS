package gr.aueb.smcs;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MapWorker {

    Socket socketWithMaster;
    ObjectOutputStream outMaster;
    ObjectInputStream inMaster;
    String messageWithMaster;

    Socket socketWithReducer;
    ObjectOutputStream outReducer;
    ObjectInputStream inReducer;
    String messageWithReducer;

    List<Directions> localDatabase;
    List<Directions> currentTaskDirectionsList;


    public MapWorker() {
	socketWithMaster = null;
	outMaster = null;
	inMaster = null;

	socketWithReducer = null;
	outReducer = null;
	inReducer = null;

	localDatabase = new ArrayList<>();
	currentTaskDirectionsList = new ArrayList<>();
    }

    public static void main(String args[]) {
	final MapWorker mapWorker = new MapWorker();
	mapWorker.initializeLocalDatabase();

	new Thread(new Runnable() {
	    public void run() {
		mapWorker.startMapWorker();
	    }
	}).start();
    }


    public void startMapWorker() {

	// CONNECT WITH MASTER

	//	System.out.println("Enter Master IP or press \"Enter\" to use default: ");
	//	Scanner scanner = new Scanner(System.in);
	//	String masterIP = scanner.nextLine();
	//
	//	if (masterIP == "\n") {
	//	    masterIP = Constants.IP_MASTER;
	//	}

	String masterIP = Constants.IP_MASTER;


	try {
	    socketWithMaster = new Socket(InetAddress.getByName(masterIP), Constants.MASTER_SERVERSOCKET_PORT);
	    outMaster = new ObjectOutputStream(socketWithMaster.getOutputStream());
	    inMaster = new ObjectInputStream(socketWithMaster.getInputStream());
	} catch (UnknownHostException unknownHost) {
	    System.err.println("You are trying to connect to an unknown host!");
	} catch (IOException ioException) {
	    ioException.printStackTrace();
	}

	try {
	    outMaster.writeObject("MapWorker");
	    outMaster.flush();

	    messageWithMaster = (String) inMaster.readObject();
	    System.out.println("Master says: " + "\"" + messageWithMaster + "\"");

	} catch (ClassNotFoundException classNot) {
	    System.err.println("Data received in unknown format");
	} catch (IOException e) {
	    e.printStackTrace();
	}
	// CONNECT WITH REDUCER 

	//	
	//	System.out.println("\n-------------\nEnter Reducer IP or press \"Enter\" to use default: ");
	//	Scanner scanner2 = new Scanner(System.in);
	//	String reducerIP = scanner2.nextLine();
	//	scanner2.close();
	//
	//	if (reducerIP == "\n") {
	//	    reducerIP = Constants.IP_REDUCER;
	//	}

	String reducerIP = Constants.IP_REDUCER;


	try {
	    socketWithReducer = new Socket(InetAddress.getByName(reducerIP), Constants.REDUCER_SERVERSOCKET_PORT);
	    outReducer = new ObjectOutputStream(socketWithReducer.getOutputStream());
	    inReducer = new ObjectInputStream(socketWithReducer.getInputStream());
	} catch (UnknownHostException unknownHost) {
	    System.err.println("You are trying to connect to an unknown host!");
	} catch (IOException ioException) {
	    ioException.printStackTrace();
	}
	try {
	    outReducer.writeObject("MapWorker");
	    outReducer.flush();

	    messageWithReducer = (String) inReducer.readObject();
	    System.out.println("Reducer says: " + "\"" + messageWithReducer + "\"");

	} catch (ClassNotFoundException classNot) {
	    System.err.println("Data received in unknown format");
	} catch (IOException e) {
	    e.printStackTrace();
	}
	//	scanner.close();

	// WAIT FOR TASKS
	waitForTasks();
    }




    public void waitForTasks() {

	while (true) {
	    try {
		final double sourceLat = Utils.RoundTo2Decimals((Double) inMaster.readObject());
		final double sourceLong = Utils.RoundTo2Decimals((Double) inMaster.readObject());
		final double destinationLat = Utils.RoundTo2Decimals((Double) inMaster.readObject());
		final double destinationLong = Utils.RoundTo2Decimals((Double) inMaster.readObject());

		    new Thread(new Runnable() {
			public void run() {
			    boolean isTaskFinished = false;
			    while (!isTaskFinished){

				System.out.println("******************");
				System.out.println("MapWorker received from Master a query for coordinates: " + sourceLat + ", "
					+ sourceLong + ", " + destinationLat + ", " + destinationLong);


				currentTaskDirectionsList = searchLocalDatabase(sourceLat, sourceLong, destinationLat,
					destinationLong);

				if (currentTaskDirectionsList != null) {
				    try {
					System.out.println("Sending results to reducer");
					outReducer.writeObject(currentTaskDirectionsList);
					currentTaskDirectionsList.clear();

					Thread.sleep(300);
					System.out.println("Sending ACK to Master");
					outMaster.writeObject("ACK");
					outMaster.flush();

				    } catch (IOException | InterruptedException e) {
					e.printStackTrace();
				    }
				} else if (currentTaskDirectionsList == null) {
				    System.out.println("Not  expected to happen... an empty list should have been sent when searching database... ");
				}
				isTaskFinished = true;
				System.out.println("\n******************");

			    }
			}
		    }).start();

	    } catch (ClassNotFoundException | IOException e) {
		e.printStackTrace();
	    }
	}
    }



    public List<Directions> searchLocalDatabase(double sourceLat, double sourceLong, double destinationLat,
	    double destinationLong) {

	//	List<Directions> directionsList = new ArrayList<>();

	for (Directions storedDirections : localDatabase) {

	    if (Utils.RoundTo2Decimals(storedDirections.getSourceLat()) == sourceLat &&
		    Utils.RoundTo2Decimals(storedDirections.getSourceLong()) == sourceLong &&
		    Utils.RoundTo2Decimals(storedDirections.getDestinationLat()) == destinationLat &&
		    Utils.RoundTo2Decimals(storedDirections.getDestinationLong()) == destinationLong)
	    {
		currentTaskDirectionsList.add(storedDirections);
	    }
	}
	if (currentTaskDirectionsList.isEmpty()) {
	    System.out.println(
		    "The coordinates were NOT found in mapWorkers Database, an EMPTY LIST will be returned");
	    return currentTaskDirectionsList;
	} else {
	    System.out.println("The coordinates were found in mapWorkers Database!!!");
	    return currentTaskDirectionsList;
	}
    }

    public void initializeLocalDatabase() {

	String jsonStringDirections = "";
	double sourceLat = 0;
	double sourceLong = 0;
	double destinationLat = 0;
	double destinationLong = 0;

	try {
	    Scanner scanner = new Scanner(
		    new FileReader("C:/Users/Ellen/Documents/Eclipse/workspace/SMCS/Data/MapWorkerDatabase1.txt"));
	    String line = "";

	    while (scanner.hasNextLine()) {
		line = scanner.nextLine();

		if (line.equals("END")) {
		    JSONObject jsonObjectDirections = (JSONObject) Utils.jsonParser.parse(jsonStringDirections);
		    Directions directions = new Directions(sourceLat, sourceLong, destinationLat, destinationLong,
			    jsonObjectDirections);
		    localDatabase.add(directions);
		    jsonStringDirections = "";
		    continue;

		} else {
		    sourceLat = Double.parseDouble(line.substring(0, 11));
		    sourceLong = Double.parseDouble(line.substring(12, 22));
		    destinationLat = Double.parseDouble(line.substring(23, 33));
		    destinationLong = Double.parseDouble(line.substring(34, 44));

		    //		    sourceLat = Utils.RoundTo2Decimals(sourceLat);
		    //		    sourceLong = Utils.RoundTo2Decimals(sourceLong);
		    //		    destinationLat = Utils.RoundTo2Decimals(destinationLat);
		    //		    destinationLong = Utils.RoundTo2Decimals(destinationLong);

		    jsonStringDirections += line.substring(45);
		}
	    }
	    scanner.close();
	    // System.out.println("LocalDatabase size is " +
	    // localDatabase.size());
	} catch (IOException | ParseException e) {
	    System.out.println("Problem during local-Database initialization");
	    e.printStackTrace();
	}
    }

}
