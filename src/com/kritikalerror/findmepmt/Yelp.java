package com.kritikalerror.findmepmt;

import android.content.Context;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.kritikalerror.findmepmt.R;

/**
 * Example for accessing the Yelp API.
 */
public class Yelp {

	OAuthService service;
	Token accessToken;

	public static Yelp getYelp(Context context) {
		return new Yelp(context.getString(R.string.consumer_key), context.getString(R.string.consumer_secret),
				context.getString(R.string.token), context.getString(R.string.token_secret));
	}

	/**
	 * Setup the Yelp API OAuth credentials.
	 *
	 * OAuth credentials are available from the developer site, under Manage API access (version 2 API).
	 *
	 * @param consumerKey Consumer key
	 * @param consumerSecret Consumer secret
	 * @param token Token
	 * @param tokenSecret Token secret
	 */
	public Yelp(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		this.service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey).apiSecret(consumerSecret).build();
		this.accessToken = new Token(token, tokenSecret);
	}
	
	/**
	 * Search with term, distance/rating, and location.
	 *
	 * @param term Search term
	 * @param sort Sort results by distance/rating
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @return JSON string response
	 */
	public String search(String term, int sort, double latitude, double longitude) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("ll", latitude + "," + longitude);
		request.addQuerystringParameter("sort", Integer.toString(sort));
		this.service.signRequest(this.accessToken, request);
		Response response = request.send();
		return response.getBody();
	}

	/**
	 * Search with term and location.
	 *
	 * @param term Search term
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @return JSON string response
	 */
	public String search(String term, double latitude, double longitude) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("ll", latitude + "," + longitude);
		this.service.signRequest(this.accessToken, request);
		Response response = request.send();
		return response.getBody();
	}

	/**
	 * Search with term string location.
	 *
	 * @param term Search term
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @return JSON string response
	 */
	public String search(String term, String location) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("location", location);
		this.service.signRequest(this.accessToken, request);
		Response response = request.send();
		return response.getBody();
	}
}
