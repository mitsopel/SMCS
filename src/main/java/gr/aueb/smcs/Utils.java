package gr.aueb.smcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Utils {

    static JSONParser jsonParser = new JSONParser() ; 
    static DecimalFormat decimalFormat = new DecimalFormat("##.##");


    public static double RoundTo2Decimals(double val) {
	return Double.valueOf(decimalFormat.format(val));
    }

    public static String askGoogleAPI (double sourceLat, double sourceLong,
	    double destinationLat, double destinationLong) throws IOException, ParseException{

	System.out.println("No results were found, thus Asking Google Api...");


	String JsonToString = "";

	URL url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin="
		+ sourceLat + "," + sourceLong
		+ "&destination="
		+ destinationLat + "," + destinationLong
		+ "&key="
		+ Constants.GOOGLE_API_KEY);

	System.out.println("\nThe url sent to google is: "+ url.toString());

	URLConnection urlConnection = url.openConnection();
	BufferedReader readJson = new BufferedReader(new InputStreamReader(
		urlConnection.getInputStream()));
	String inputLine;
	while ((inputLine = readJson.readLine()) != null)
	    JsonToString +=inputLine;
	readJson.close();

	//JSONObject jsonObject= (JSONObject) jsonParser.parse(JsonToString);
	// Directions directions= new Directions(jsonObject);
	//    directions.add(temp);

	return JsonToString;
    }    
    
    
    public static BigInteger calculateHash (String first, String second){

	String finalString = first+second;
	MessageDigest m = null;
	try {
	    m = MessageDigest.getInstance("MD5");
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	    System.out.println("Invalid hashing algorithm");
	}
	m.reset();
	m.update(finalString.getBytes());
	byte[] digest = m.digest();
	BigInteger bigInt = new BigInteger(1,digest);

	return bigInt;
    }

    public static void main (String args[]) {

	String stringDirections = null;
	PrintWriter printWriter = null;

	try {
	    double sourceLat = 40.78444444;
	    double sourceLong = 22.9222222;
	    double destinationLat = 40.8277777;
	    double destinationLong = 22.9944444;

	    stringDirections = askGoogleAPI(sourceLat,sourceLong,destinationLat,destinationLong);
	    System.out.println(stringDirections);

	    File outFile = new File("C:/Users/Ellen/Documents/Eclipse/workspace/SMCS/Data/MapWorkerDatabase1.txt");
	    outFile.getParentFile().mkdirs();
	    outFile.createNewFile();

	    FileWriter fileWriter = new FileWriter(outFile, true);
	    printWriter = new PrintWriter(fileWriter);
	    printWriter.println(sourceLat +"," + sourceLong + "," +destinationLat +"," + destinationLong +"," + stringDirections);
	    printWriter.println("END");

	} 
	catch (IOException e) {
	    e.printStackTrace();
	} 
	catch (ParseException e) {
	    e.printStackTrace();
	} finally {
	    if (printWriter != null) {
		printWriter.close();
	    }
	}

    }
}
