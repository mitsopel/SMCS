package gr.aueb.smcs;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Directions implements Serializable {

    private static final long serialVersionUID = 2540262674905767116L;

    double sourceLat;
    double sourceLong;
    double destinationLat;
    double destinationLong;
    JSONObject jsonObjectDirections;

    public Directions(double sourceLat, double sourceLong, double destinationLat,
	    double destinationLong, JSONObject jsonObjectDirections) {

	this.sourceLat = sourceLat;
	this.sourceLong = sourceLong;
	this.destinationLat = destinationLat;
	this.destinationLong = destinationLong;

	this.jsonObjectDirections = jsonObjectDirections;

    }

//    public Directions(JSONObject object) {
//
//	this.jsonObjectDirections = (JSONObject) object;
//
//	JSONArray jsonObject1 = (JSONArray) object.get("routes");
//	JSONObject jsonObject2 = (JSONObject) jsonObject1.get(0);
//	JSONArray jsonObject3 = (JSONArray) jsonObject2.get("legs");
//	JSONObject jsonObject4 = (JSONObject) jsonObject3.get(0);
//	JSONObject jsonObject5 = (JSONObject) jsonObject4.get("end_location");
//	JSONObject jsonObject6 = (JSONObject) jsonObject4.get("start_location");
//	this.sourceLat = (Double) jsonObject6.get("lat");
//	this.sourceLong = (Double) jsonObject6.get("lng");
//	this.destinationLat = (Double) jsonObject5.get("lat");
//	this.destinationLong = (Double) jsonObject5.get("lng");
//
//    }

    public double getSourceLat() {
	return sourceLat;
    }

    public void setSourceLat(double sourceLat) {
	this.sourceLat = sourceLat;
    }

    public double getSourceLong() {
	return sourceLong;
    }

    public void setSourceLong(double sourceLong) {
	this.sourceLong = sourceLong;
    }

    public double getDestinationLat() {
	return destinationLat;
    }

    public void setDestinationLat(double destinationLat) {
	this.destinationLat = destinationLat;
    }

    public double getDestinationLong() {
	return destinationLong;
    }

    public void setDestinationLong(double destinationLong) {
	this.destinationLong = destinationLong;
    }

}
