package com.andrewchalmers.app;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import java.net.UnknownHostException;
import java.util.List;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserMentionEntity;
import twitter4j.conf.ConfigurationBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.io.File;

public class Streamer {

	private ConfigurationBuilder cb;
	private static DB db;
	private static DBCollection items;
	static String keyword = "bluntz";

	/**
	 * static block used to construct a connection with tweeter with twitter4j
	 * configuration with provided settings. This configuration builder will be
	 * used for next search action to fetch the tweets from twitter.com.
	 */

	public static void main(String[] args) throws InterruptedException {
		try {
			// on constructor load initialize MongoDB and load collection
			initMongoDB();
			items = db.getCollection(keyword);
			BasicDBObject index = new BasicDBObject("tweet_ID", 1);
			if (items.find(index) == null) {
				items.createIndex(index, new BasicDBObject("unique", true));
			}

		} catch (MongoException ex) {
			System.out.println("MongoException :" + ex.getMessage());
		}
		Streamer taskObj = new Streamer();
		taskObj.loadMenu();
	}

	public void loadMenu() throws InterruptedException {
		System.out.print("Please choose your Keyword:\t");
		int i = 0;
		while(i < 1)
		{
			cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true);
			getTweetByQuery(true,keyword);
			cb = null;
			Thread.sleep(60 * 1000);              // wait
		}
	}

	/**
	 * initMongoDB been called in constructor so every object creation this
	 * initialize MongoDB.
	 */
	public static void initMongoDB() throws MongoException {
		try {
			System.out.println("Connecting to Mongo DB..");
			Mongo mongo = new MongoClient();  
			db = mongo.getDB("tweetDB2");
		} catch (UnknownHostException ex) {
			System.out.println("MongoDB Connection Error :" + ex.getMessage());
		}
	}

	public void getTweetByQuery(boolean loadRecords, String keyword) throws InterruptedException {

		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		if (cb != null) {
			try {
				Query query = new Query(keyword);
				query.setCount(100);
				QueryResult result;
				result = twitter.search(query);
				System.out.println("Getting Tweets...");
				List<Status> tweets = result.getTweets();

				for (Status tweet : tweets) {
					BasicDBObject basicObj = new BasicDBObject();
					basicObj.put("user_name", tweet.getUser().getScreenName());
					basicObj.put("retweet_count", tweet.getRetweetCount());
					basicObj.put("tweet_followers_count", tweet.getUser().getFollowersCount());
					basicObj.put("source",tweet.getSource());
					//basicObj.put("coordinates",tweet.getGeoLocation());
					UserMentionEntity[] mentioned = tweet.getUserMentionEntities();
					basicObj.put("tweet_mentioned_count", mentioned.length);
					basicObj.put("tweet_ID", tweet.getId());
					basicObj.put("tweet_text", tweet.getText());

					try {
						items.insert(basicObj);
					} catch (Exception e) {
						System.out.println("MongoDB Connection Error : " + e.getMessage());
					}
				}

				// Printing fetched records from DB.
				if (loadRecords) {
					getTweetsRecords();
				}

			} catch (TwitterException te) {
				System.out.println("te.getErrorCode() " + te.getErrorCode());
				System.out.println("te.getExceptionCode() " + te.getExceptionCode());
				System.out.println("te.getStatusCode() " + te.getStatusCode());
				if (te.getStatusCode() == 401) {
					System.out.println("Twitter Error : \nAuthentication credentials (https://dev.twitter.com/pages/auth) were missing or incorrect.\nEnsure that you have set valid consumer key/secret, access token/secret, and the system clock is in sync.");
				} else {
					System.out.println("Twitter Error : " + te.getMessage());
				}
			}
		} else {
			System.out.println("MongoDB is not Connected! Please check mongoDB intance running..");
		}
	}

	public void getTweetsRecords() throws InterruptedException {
		BasicDBObject fields = new BasicDBObject("_id", true).append("user_name", true).append("tweet_text", true);
		DBCursor cursor = items.find(new BasicDBObject(), fields);

		while (cursor.hasNext()) {
			System.out.println(cursor.next());
		}

	}

	public static void read(){

		try {
			File fXmlFile = new File("/Users/jaclynfoss/tickers.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("industry");
			System.out.println(nList.getLength());

			for (int temp = 0; temp < nList.getLength(); temp++) {

				NodeList companies = nList.item(temp).getChildNodes();
				if (companies != null) { 
					for (int i = 0; i < companies.getLength(); i++) {

						if (companies.item(i).getNodeType() == Node.ELEMENT_NODE) {
							BasicDBObject basicObj = new BasicDBObject();
							String cName = companies.item(i).getAttributes().getNamedItem("name").toString();
							basicObj.put("name",companies.item(i).getAttributes().getNamedItem("name"));
							basicObj.put("ticker", companies.item(i).getAttributes().getNamedItem("symbol"));
							basicObj.put("industry", nList.item(temp).getAttributes().getNamedItem("name"));
							keyword = cName;
							Streamer taskObj = new Streamer();
							taskObj.loadMenu();

						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
