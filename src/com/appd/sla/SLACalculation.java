package com.appd.sla;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;

public class SLACalculation {
  
	
	public static void main(String[] args) throws IOException {
		
		// Retrieve account info and REST info
		String user = null;
		String pass = null;
		String getSQLSLA = null;
		//String getSQLSLAPass = null;
		String eventsSchemaURL = null;
		String eventsPublishURL = null;
		String eventsQueryURL = null;
		String getApiKey = null;    // this is for the schema we are going to read from
		String postApiKey = null;    //
		String accountName = null;
		String contentType = null;
		String createSchema = null;
		String businessTransactions = null;
		String application = null;  // Customer Service, WebStore, etc.
		String customerName = null;
		String dataCenter = null;
		String btSLA = null;
		int valueSLAMiss = 0;
		int valueSLAPass = 0;
		int count = 0;
		String[] config = new String[16];
		String line = null;
		String path = "./properties.txt";
		HttpClient client = null;
		
		// Get configuration from properties file
		FileInputStream fis = new FileInputStream(path);
		BufferedReader in = new BufferedReader(new InputStreamReader(fis));
		 while ((line = in.readLine()) != null) {
	           config[count] = line;
	           count++;
		 }
		 in.close();
		 /* Legacy - custom metric
		
		 postURL = config[4]; //legacy
		 metricName = config[5]; //legacy
		 */
		 user = 				config[0]; 
		 pass = 				config[1];
		 getSQLSLA = 			config[2];
		 eventsSchemaURL =		config[3];
		 eventsPublishURL = 	config[4];
		 eventsQueryURL = 		config[5];
		 getApiKey = 			config[6];
		 postApiKey =			config[7];
		 accountName = 			config[8];
		 contentType = 			config[9];
		 businessTransactions = config[10]; // "login","checkout"
		 application =			config[11]; // "Fulfillment", "Web Store"
		 customerName =			config[12]; // "Wertner Tools", "B & B Distribution"
		 dataCenter =			config[13]; // "Raleigh", "Dallas"
		 createSchema = 		config[14];
		 btSLA = 				config[15];

		CredentialsProvider provider = new BasicCredentialsProvider();
    	UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, pass);
		provider.setCredentials(AuthScope.ANY, credentials);
	//	HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).setConnectionTimeToLive(1, TimeUnit.MINUTES).build();
	
		
		//createSchema(client,eventsSchemaURL,postApiKey,accountName,createSchema); //need an httpget query to see if it exists
		String [] allBT = businessTransactions.split(",");
		String [] allSLA = btSLA.split(",");
		String [] allDC = dataCenter.split(",");
		String [] allApp = application.split(",");
		String [] allCust = customerName.split(",");
		// BT loop
		// bt, customerName, and dataCenter all needs to be in ""
		for(int l = 0; l<allDC.length; l++)
		{
			for(int k = 0; k<allApp.length; k++)
			{	
				for(int j = 0; j<allCust.length; j++)
				{ 
					for(int i = 0; i<allBT.length; i++)
					{
						client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).setConnectionTimeToLive(1, TimeUnit.MINUTES).build();
			
						System.out.println(k);
						System.out.println(j);
						System.out.println(i);
						valueSLAMiss = getData(client,eventsQueryURL,getApiKey,accountName,contentType,createNewSql(getSQLSLA," > ",allSLA[i],allBT[i],allCust[j],allApp[k],allDC[l]));
						//client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).setConnectionTimeToLive(1, TimeUnit.MINUTES).build();
			
						System.out.println("SLAMiss is done");
						valueSLAPass = getData(client,eventsQueryURL,getApiKey,accountName,contentType,createNewSql(getSQLSLA," < ",allSLA[i],allBT[i],allCust[j],allApp[k],allDC[l]));
						System.out.println("SLAPass is done");
						//client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).setConnectionTimeToLive(1, TimeUnit.MINUTES).build();
			
						postSchema(client,eventsPublishURL,postApiKey,accountName,contentType,valueSLAMiss,valueSLAPass,allBT[i],allCust[j],allApp[k],allDC[l]);
						HttpClientUtils.closeQuietly(client);
					}
				}
			}
		}
				
		// loop through the bt list and add in the bt and after that, add in the static string - & starttime = (dt.getMillis()-3600000) & endtime = dt.getMillis()
		
		/*
		 * Legacy code for pushing to a custom metric
		 * 
		// Calculate the percentage of calls > a threshold
		String result = calculatePercentage(valueTotalCall,valueNumCallThreshold);
		HttpPost post = new HttpPost(postURL);
		post.setEntity(postResult(result,metricName));
		post.setHeader("Accept","application/json");
		post.setHeader("Content-Type","application/json");
		
		HttpResponse postResult = client.execute(post);
		System.out.println(postResult.getStatusLine().getStatusCode());
		*/
	}
	
	public static void createSchema(HttpClient client,String connectionString, String postApiKey, String accountName , String sqlString) throws ClientProtocolException, IOException {
		HttpGet get = new HttpGet(connectionString);
		//get.getURI()
		HttpPost post = new HttpPost(connectionString);
		post.addHeader("X-Events-API-AccountName",accountName);
		post.addHeader("X-Events-API-Key",postApiKey);
		post.setEntity(new StringEntity(sqlString));
		HttpResponse postResult = client.execute(post);
		System.out.println(postResult.getStatusLine().getStatusCode());
		//flag = true
		//Example
		//connectionString = "localhost:9080/events/schema/SLA_Test_2"
		//postApiKey = -H"X-Events-API-AccountName:customer1_c7710d80-aa90-4ec4-a7df-6ca8ccf94746" -H"X-Events-API-Key:0f6025c7-26ef-42e7-bd84-b81cac00122c" -H"Content-type: application/vnd.appd.events+json;v=1" 
		//sqlString = -d '{"schema" : { "BT": "string", "SLAPass": "integer", "SLAMiss": "integer", "SLAAttain": "float"}}'
		
	}
	
	public static String createNewSql(String sql, String compare, String slaValue, String bt, String custName, String app, String dc) {
		DateTime dt = new DateTime();
		long startDate = dt.getMillis()-3600000;
		System.out.println(dc);
		System.out.println(custName);
		System.out.println(bt);
		return sql + compare + slaValue + " AND BT = " + bt + " AND CustomerName = " + custName + " AND DataCenter = " + dc + " AND eventTimestamp > " + Long.toString(startDate) + " AND eventTimestamp < " + Long.toString(dt.getMillis());
	}
	
	public static Integer getData(HttpClient client,String connectionString, String getApiKey, String accountName, String contentType, String sqlString) {
		System.out.println(connectionString);
		System.out.println(sqlString);
		
		HttpPost post = new HttpPost(connectionString);
		
		post.addHeader("X-Events-API-AccountName",accountName);
		post.addHeader("X-Events-API-Key",getApiKey);
		post.addHeader("Content-type",contentType);
		HttpResponse postResult = null;
		int result = 0;
		try {
		post.setEntity(new StringEntity(sqlString));
		System.out.println("About to GET from Event Service");
		postResult = client.execute(post);
		result = parseResults(getMessage(postResult));
		//Thread.sleep(10000);
		System.out.println("Fetched from Event Service - Status is " + postResult.getStatusLine().getStatusCode() );
		return result;
		} catch (Throwable ex){
			ex.printStackTrace();
			return -1;
	
		} finally {
				HttpClientUtils.closeQuietly(postResult);
		}
		//return parseResults(postResult.getEntity().toString());
		// example result
		//{"fields":[{"label":"count(*)","field":"*","type":"integer","aggregation":"count"}],"results":[[378]],"total":1,"schema":"bt_resptime_2"}
		
		//delimit on "results": then on comma and remove square brackets to get the sla number
		
	}
	
	public static void postSchema(HttpClient client,String connectionString, String postApiKey, String accountName, String contentType, int SLAMiss, int SLAPass, String bt, String custName, String app, String dc) throws ClientProtocolException, IOException {
		System.out.println(connectionString);
		HttpPost post = new HttpPost(connectionString);
		DateTime dt = new DateTime();
		String postSQL = null;
		
		postSQL = "[{\"eventTimestamp\": \"" + dt.now() + "\",\"BT\": " + bt + ",\"CustomerName\": " + custName + ",\"ApplicationName\": " + app + ",\"DataCenter\": " + dc + ",\"SLAPass\": " + SLAPass +
				",\"SLAMiss\": " + SLAMiss + ",\"SLAAttain\": " + calculatePercentage(SLAMiss,SLAPass+SLAMiss) + "}]";
		post.addHeader("X-Events-API-AccountName",accountName);
		post.addHeader("X-Events-API-Key",postApiKey);
		post.addHeader("Content-type",contentType);
		post.setEntity(new StringEntity(postSQL));
		System.out.println("About to POST to Event Service");
		HttpResponse postResult = client.execute(post);
		System.out.println(postResult.getEntity().toString());
		System.out.println("POSTed to Event Service - Status is " + postResult.getStatusLine().getStatusCode() );
		
		//Example
		//connectionString = "localhost:9080/events/publish/SLA_Test_2" 
		//postApiKey = -H"X-Events-API-AccountName:customer1_c7710d80-aa90-4ec4-a7df-6ca8ccf94746" -H"X-Events-API-Key:0f6025c7-26ef-42e7-bd84-b81cac00122c" -H"Content-type: application/vnd.appd.events+json;v=1" 
		//Dynamically created content - -d '[{"eventTimestamp": "2016-08-24T04:00:00.000Z
		//sqlString = ","BT": "Login","SLAPass": 971,"SLAMiss": 81,"SLAAttain": 92.300}]'
		
		//sql string will eventually be dynamics 
		// change arguments to include SLAPass and SLAMiss to be read in from rest api then call calculate percentage
		
		
	}
	
	public static String calculatePercentage(int SLAMiss, int TotalCall) {
		Double value = 100*(1-((double)SLAMiss/(double)TotalCall));
//		int result = value.intValue();
		return Double.toString(value);
		
	}
	
	public static Integer parseResults(String value) {
		System.out.println(value);
		return Integer.valueOf(value.split("\"results\":")[1].split(",")[0].replace("[[", "").replace("]]", ""));
	}
	
	public static String getMessage(HttpResponse response) throws UnsupportedOperationException, IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuilder total = new StringBuilder();
		String line = null;
		
		while ((line = r.readLine()) != null) {
		//System.out.println(line);
		total.append(line);
		}
		r.close();	
		return total.toString();
	}
	
	
	//legacy - custom transaction metric
	public static Double parseValue(String value) {
		value = value.replace("<value>","");
		value = value.replace("</value>", "");
		System.out.println(value);
		return Double.valueOf(value);
	}
	
	//legacy - custom transaction metric
	public static Double getSLA(HttpClient client, String restURL) throws UnsupportedOperationException, IOException {
		
		HttpResponse response = client.execute(new HttpGet(restURL));
		return parseValue(getMessage(response).replaceAll("\\s",""));
		
	}
	
	//legacy - risk of bad aggregation 
	public static StringEntity postResult(String value,String metricName) throws UnsupportedEncodingException {
		String entity = "["
				+ "{" +
				"\"metricName\":" + metricName + "," + 
				"\"aggregatorType\": \"OBSERVATION\"," +
				"\"value\":  \"" + value + "\"" +
				"}" +
				"]";
		System.out.println(value);
		return new StringEntity(entity);
	}
  
}