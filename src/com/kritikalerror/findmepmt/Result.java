package com.kritikalerror.findmepmt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  Model class for Yelp data.
 * 
 * @author Michael Hii
 * @Date   7/29/2014
 *
 */
public class Result {
	private static final String GEOCODE_URI = "http://maps.google.com/maps/api/geocode/json?address=";
	
    private String id;
    private String icon;
    private String name; //"name"
    private String address; //"display_address"
    private String phone; //"display_phone"
    private String rating; //"rating"
    private Double latitude; //"coordinate.latitude"
    private Double longitude; //"coordinate.longitude"
    private String isOpen = "false"; //"is_closed"
    private String imgUrl;
    private String mobileUrl;
    
    /*
     * Constructor for Yelp Result class
     * "name" and "address" parameters cannot be null because they are required
     */
    public Result()
    {
    	this.name = "none";
    	this.address = "none";
    	this.latitude = 0.0;
    	this.longitude = 0.0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    public void appendCity(String city) {
    	this.address = this.address + ", " + city;
    }
    
    public void appendState(String state) {
    	this.address = this.address + ", " + state;
    }
    
    public void setOpen(String open) {
    	if(open.equals("false"))
    	{
    		this.isOpen = "Currently Closed";
    	}
    	else
    	{
    		this.isOpen = "Currently Open";
    	}
    }
    
    public String isOpen() {
    	return this.isOpen;
    }
    
    public void setRating(String rating) {
    	this.rating = rating;
    }
    
    public String getRating() {
    	return this.rating;
    }
    
    public void setImage(String link) {
    	this.imgUrl = link;
    }
    
    public String getImage() {
    	return this.imgUrl;
    }
    
    public void setMobileUrl(String link) {
    	this.mobileUrl = link;
    }
    
    public String getMobileUrl() {
    	return this.mobileUrl;
    }


    public static Result jsonToClass(JSONObject business) {
        try 
        {
            Result result = new Result();
            JSONObject locations = business.getJSONObject("location");
            
            // Set mandatory members
            if(business.has("name")) 
            {
            	result.setName(business.getString("name"));
            }

            if(locations.has("address") && (locations.getJSONArray("address").length() > 0)) 
            {
            	result.setAddress(locations.getJSONArray("address").getString(0));
            }
            
            if(locations.has("city")) 
            {
            	result.appendCity(locations.getString("city"));
            }
            
            if(locations.has("state_code")) 
            {
            	result.appendState(locations.getString("state_code"));
            }
            
            // Get coordinates either from JSON or Google's geocode service
            if(locations.has("coordinate"))
			{
				result.setLatitude(locations.getJSONObject("coordinate").getDouble("latitude"));
				result.setLongitude(locations.getJSONObject("coordinate").getDouble("longitude"));
			}
            else
            {
            	result.setLatLongFromAddr();
            }
            
            // Set optional members
            if(business.has("rating"))
            {
            	result.setRating(business.getString("rating"));
            }
            
            if(business.has("phone"))
            {
            	result.setPhone(business.getString("phone"));
            }
            
            if(business.has("is_closed"))
            {
            	result.setOpen(business.getString("is_closed"));
            }
            
            if(business.has("mobile_url"))
            {
            	result.setMobileUrl(business.getString("mobile_url"));
            }
            
            if(business.has("rating_img_url_large"))
            {
            	result.setOpen(business.getString("rating_img_url_large"));
            }
            return result;
        } 
        catch (JSONException ex) 
        {
            Logger.getLogger(Result.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private void setLatLongFromAddr()
    {
    	if(!this.address.equals("none"))
    	{
	    	try {
	    		// Prepare the URL
				URL url = new URL(GEOCODE_URI + URLEncoder.encode(this.address, "UTF-8"));
				
				// Prepare the connection
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				
				// Connect and get results
				conn.connect();
				String responseString = convertStreamToString(conn.getInputStream());
				JSONObject response = new JSONObject(responseString);
				this.parseJSONLatLong(response.getJSONArray("results").getJSONObject(0));
				conn.disconnect();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    private void parseJSONLatLong(JSONObject response)
    {
    	JSONObject geometry;
		try {
			geometry = (JSONObject) response.get("geometry");
			JSONObject location = (JSONObject) geometry.get("location");
			this.setLatitude(location.getDouble("lat"));
            this.setLongitude(location.getDouble("lng"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @SuppressWarnings("resource")
	private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public String toString() {
        return "Place{" + "id=" + id + ", address=" + address + ", name=" + name + ", latitude=" + latitude + ", longitude=" + longitude + '}';
    }
}