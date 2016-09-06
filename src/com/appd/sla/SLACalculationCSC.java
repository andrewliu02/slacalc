package com.appd.sla;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class SLACalculationCSC {
  
	
	public static void main(String[] args) throws IOException {
		
		// Retrieve account info and REST info
		String user = null;
		String pass = null;
		String getURLTotalCall = null;
		String getURLNumCallThreshold = null;
		String postURL = null;
		String metricName = null;
		
		int count = 0;
		String[] config = new String[6];
		String line = null;
		String path = "./properties.txt";
		
		// Get configuration from properties file
		FileInputStream fis = new FileInputStream(path);
		BufferedReader in = new BufferedReader(new InputStreamReader(fis));
		 while ((line = in.readLine()) != null) {
	           config[count] = line;
	           count++;
		 }
		 in.close();
		 
		 user = config[0];
		 pass = config[1];
		 getURLTotalCall = config[2];
		 getURLNumCallThreshold = config[3];
		 postURL = config[4];
		 metricName = config[5];
		 
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, pass);
		provider.setCredentials(AuthScope.ANY, credentials);
		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		
		// Needs 2 responses to get the total number and the number that is > X and then simple division
		HttpResponse response = client.execute(new HttpGet(getURLTotalCall));
		Double valueTotalCall = parseValue(getMessage(response).replaceAll("\\s",""));
		
		HttpResponse response2 = client.execute(new HttpGet(getURLNumCallThreshold));
		Double valueNumCallThreshold = parseValue(getMessage(response2).replaceAll("\\s",""));
		
		// Calculate the percentage of calls > a threshold
		String result = calculatePercentage(valueTotalCall,valueNumCallThreshold);
		HttpPost post = new HttpPost(postURL);
		post.setEntity(postResult(result,metricName));
		post.setHeader("Accept","application/json");
		post.setHeader("Content-Type","application/json");
		
		HttpResponse postResult = client.execute(post);
		System.out.println(postResult.getStatusLine().getStatusCode());
	}
	
	public static String getMessage(HttpResponse response) throws UnsupportedOperationException, IOException {
		
		BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuilder total = new StringBuilder();
		String line = null;
		
		while ((line = r.readLine()) != null) {
			if(line.contains("<value>"))
			{
				total.append(line);
			}	
		}
		r.close();	
		return total.toString();
	}
	
	public static Double parseValue(String value) {
		value = value.replace("<value>","");
		value = value.replace("</value>", "");
		System.out.println(value);
		return Double.valueOf(value);
	}
	
	public static String calculatePercentage(Double valueTotalCall, Double valueNumCallThreshold) {
		Double value = (1-(valueNumCallThreshold/valueTotalCall))*10000000;
		int result = value.intValue();
		return Integer.toString(result);
		
	}
	
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