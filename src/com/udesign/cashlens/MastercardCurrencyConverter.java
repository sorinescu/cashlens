/*******************************************************************************
 * Copyright 2012 Sorin Otescu <sorin.otescu@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.udesign.cashlens;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.xmlpull.v1.XmlPullParser;

import com.udesign.cashlens.CashLensStorage.Currency;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Xml;

final class MastercardCurrencyConverter extends CurrencyConverter
{
	// Fake trust manager, allowing us to ignore SSL certificate errors.
	// @see http://groups.google.com/group/android-developers/browse_thread/thread/62d856cdcfa9f16e?pli=1
	private static class FakeX509TrustManager implements X509TrustManager 
	{
		private static TrustManager[] mTrustManagers; 
        private static final X509Certificate[] mAcceptedIssuers = new 
        		X509Certificate[] {};
        
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException	{}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException	{}

		public X509Certificate[] getAcceptedIssuers()
		{
			return mAcceptedIssuers;
		}
		
        public static void allowAllSSL() 
        { 
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() 
            { 
					public boolean verify(String hostname, SSLSession session)
					{
						return true;
					} 
            });
            
            SSLContext context = null; 
            if (mTrustManagers == null)
            	mTrustManagers = new TrustManager[] { new FakeX509TrustManager() }; 

            try 
            { 
	            context = SSLContext.getInstance("TLS"); 
	            context.init(null, mTrustManagers, new SecureRandom()); 
            } 
            catch (NoSuchAlgorithmException e) { 
                    e.printStackTrace(); 
            } 
            catch (KeyManagementException e) { 
                    e.printStackTrace(); 
            } 
            
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory()); 
        } 
	} 
	
	MastercardCurrencyConverter()
	{
		FakeX509TrustManager.allowAllSSL();
	}
	
	@Override
	public String name()
	{
		return "MasterCard";
	}

	@Override
	public String description()
	{
		return "";
	}

	/* (non-Javadoc)
	 * @see com.udesign.cashlens.CurrencyConverter#icon(android.content.Context)
	 */
	@Override
	public Drawable icon(Context context)
	{
		return context.getResources().getDrawable(R.drawable.mastercard);
	}

	private HashMap<Integer,Float> readRatesFromXML(InputStream xmlStream)
	{
		XmlPullParser xmlParser = Xml.newPullParser();
		HashMap<Integer,Float> currencyToRate = new HashMap<Integer, Float>();
		Currency currency = null;
		
		try
		{
			// We aren't the first ones to call instance(), so we can pass a null context
			CashLensStorage storage = CashLensStorage.instance(null);
			
			xmlParser.setInput(xmlStream, null);
			
			int eventType = xmlParser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT)
			{
		        if (xmlParser.getEventType() == XmlPullParser.START_TAG) 
		        {
	                String s = xmlParser.getName();
	 
	                if (s.equalsIgnoreCase("ALPHA_CURENCY_CODE"))
	                	currency = storage.getCurrencyByCode(xmlParser.nextText());
	                else if (s.equalsIgnoreCase("CONVERSION_RATE"))
	                {
	                	float rate = Float.parseFloat(xmlParser.nextText());
	                	currencyToRate.put(currency.id, rate);
	                	currency = null;
	                }
		        } 
		 
		        eventType = xmlParser.next();
			}
		}
		catch (Exception e) 
		{
			return null;
		}
		
		return currencyToRate;
	}
	
	@Override
	protected void loadExchangeRates(ExchangeRates rates)
	{
		StringBuffer urlParameters = new StringBuffer();
		
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		
		urlParameters.append("service=getExchngRateDetails");
		urlParameters.append("&baseCurrency=" + rates.baseCurrency.code);
		urlParameters.append("&settlementDate=" + format.format(rates.gmtDate));
		
		try
		{
			HttpsURLConnection con = (HttpsURLConnection) new URL(
					"https://www.mastercard.com/psder/eu/callPsder.do").openConnection();
			con.setRequestMethod("POST"); 
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			con.setRequestProperty("Content-Length", Integer.toString(urlParameters.length())); 
			con.setRequestProperty("Content-Language", "en-US"); 
			con.setRequestProperty("Connection", "close"); 
			con.setUseCaches (false); 
			con.setDoOutput(true); 
			con.setDoInput(true); 
	
			// Send request 
			DataOutputStream wr = new DataOutputStream(con.getOutputStream()); 
			wr.writeBytes(urlParameters.toString()); 
			wr.flush(); 
			wr.close(); 
			
			con.disconnect();
			
			InputStream rd = con.getInputStream();
			rates.currencyIdToRate = readRatesFromXML(rd);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
	}
}
