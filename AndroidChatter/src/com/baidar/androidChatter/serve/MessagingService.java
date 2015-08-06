/* 
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidar.androidChatter.serve;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.baidar.androidChatter.LoggingIn;
import com.baidar.androidChatter.PerformingMessaging;
import com.baidar.androidChatter.R;
import com.baidar.androidChatter.comm.Socketer;
import com.baidar.androidChatter.interfacer.Manager;
import com.baidar.androidChatter.interfacer.SocketerInterface;
import com.baidar.androidChatter.interfacer.Updater;
import com.baidar.androidChatter.toolBox.ControllerOfFriend;
import com.baidar.androidChatter.toolBox.StorageManipulater;
import com.baidar.androidChatter.toolBox.MessageController;
import com.baidar.androidChatter.toolBox.HandlerXML;
import com.baidar.androidChatter.typo.InfoOfFriend;
import com.baidar.androidChatter.typo.InfoOfMessage;


/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application.  The {@link LocalServiceController}
 * and {@link LocalServiceBinding} classes show how to interact with the
 * service.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class MessagingService extends Service implements Manager, Updater {
//	private NotificationManager mNM;
	
	public static String USERNAME;
	public static final String TAKE_MESSAGE = "Take_Message";
	public static final String FRIEND_LIST_UPDATED = "Take Friend List";
	public static final String MESSAGE_LIST_UPDATED = "Take Message List";
	public ConnectivityManager conManager = null; 
	private final int UPDATE_TIME_PERIOD = 15000;

	private String rawFriendList = new String();
	private String rawMessageList = new String();

	SocketerInterface socketOperator = new Socketer(this);

	private final IBinder mBinder = new IMBinder();
	private String username;
	private String password;
	private boolean authenticatedUser = false;
	 // timer to take the updated data from server
	private Timer timer;
	

	private StorageManipulater localstoragehandler; 
	
	private NotificationManager mNM;

	public class IMBinder extends Binder {
		public Manager getService() {
			return MessagingService.this;
		}
		
	}
	   
    @Override
    public void onCreate() 
    {   	
         mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

         localstoragehandler = new StorageManipulater(this);
        // Display a notification about us starting.  We put an icon in the status bar.
     //   showNotification();
    	conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    	new StorageManipulater(this);
    	
    	// Timer is used to take the friendList info every UPDATE_TIME_PERIOD;
		timer = new Timer();   
		
		Thread thread = new Thread()
		{
			@Override
			public void run() {			
				
				
				Random random = new Random();
				int tryCount = 0;
				while (socketOperator.startListening(10000 + random.nextInt(20000))  == 0 )
				{		
					tryCount++; 
					if (tryCount > 10)
					{
						// if it can't listen a port after trying 10 times, give up...
						break;
					}
					
				}
			}
		};		
		thread.start();
    
    }

/*
    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }
*/	

	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}




	/**
	 * Show a notification while this service is running.
	 * @param msg 
	 **/
    private void showNotification(String username, String msg) 
	{       
        // Set the icon, scrolling text and TIMESTAMP
    	String title = "AndroidIM: You got a new Message! (" + username + ")";
 				
    	String text = username + ": " + 
     				((msg.length() < 5) ? msg : msg.substring(0, 5)+ "...");
    	
    	//NotificationCompat.Builder notification = new NotificationCompat.Builder(R.drawable.stat_sample, title,System.currentTimeMillis());
    	NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
    	.setSmallIcon(R.drawable.notification)
    	.setContentTitle(title)
    	.setContentText(text); 
    	
    	

        Intent i = new Intent(this, PerformingMessaging.class);
        i.putExtra(InfoOfFriend.USERNAME, username);
        i.putExtra(InfoOfMessage.MESSAGETEXT, msg);	
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                i, 0);

        // Set the info for the views that show in the notification panel.
        // msg.length()>15 ? MSG : msg.substring(0, 15);
        mBuilder.setContentIntent(contentIntent); 
        
        mBuilder.setContentText("New message from " + username + ": " + msg);
        
        //TODO: it can be improved, for instance message coming from same user may be concatenated 
        // next version
        
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify((username+msg).hashCode(), mBuilder.build());
    }
	 

	public String getUsername() {
		return this.username;
	}

	
	public String sendMessage(String  username, String  tousername, String message) throws UnsupportedEncodingException 
	{			
		String params = "username="+ URLEncoder.encode(this.username,"UTF-8") +
						"&password="+ URLEncoder.encode(this.password,"UTF-8") +
						"&to=" + URLEncoder.encode(tousername,"UTF-8") +
						"&message="+ URLEncoder.encode(message,"UTF-8") +
						"&action="  + URLEncoder.encode("sendMessage","UTF-8")+
						"&";		
		Log.i("PARAMS", params);
		return socketOperator.sendHttpRequest(params);		
	}

	
	private String getFriendList() throws UnsupportedEncodingException 	{		
		// after authentication, server replies with friendList xml
		
		 rawFriendList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
		 if (rawFriendList != null) {
			 this.parseFriendInfo(rawFriendList);
		 }
		 return rawFriendList;
	}
	
	private String getMessageList() throws UnsupportedEncodingException 	{		
		// after authentication, server replies with friendList xml
		
		 rawMessageList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
		 if (rawMessageList != null) {
			 this.parseMessageInfo(rawMessageList);
		 }
		 return rawMessageList;
	}
	
	

	/**
	 * authenticateUser: it authenticates the user and if succesful
	 * it returns the friend list or if authentication is failed 
	 * it returns the "0" in string type
	 * @throws UnsupportedEncodingException 
	 * */
	public String authenticateUser(String usernameText, String passwordText) throws UnsupportedEncodingException 
	{
		this.username = usernameText;
		this.password = passwordText;	
		
		this.authenticatedUser = false;
		
		String result = this.getFriendList(); //socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
		if (result != null && !result.equals(LoggingIn.AUTHENTICATION_FAILED)) 
		{			
			// if user is authenticated then return string from server is not equal to AUTHENTICATION_FAILED
			this.authenticatedUser = true;
			rawFriendList = result;
			USERNAME = this.username;
			Intent i = new Intent(FRIEND_LIST_UPDATED);					
			i.putExtra(InfoOfFriend.FRIEND_LIST, rawFriendList);
			sendBroadcast(i);
			
			timer.schedule(new TimerTask()
			{			
				public void run() 
				{
					try {					
						//rawFriendList = IMService.this.getFriendList();
						// sending friend list 
						Intent i = new Intent(FRIEND_LIST_UPDATED);
						Intent i2 = new Intent(MESSAGE_LIST_UPDATED);
						String tmp = MessagingService.this.getFriendList();
						String tmp2 = MessagingService.this.getMessageList();
						if (tmp != null) {
							i.putExtra(InfoOfFriend.FRIEND_LIST, tmp);
							sendBroadcast(i);	
							Log.i("friend list broadcast sent ", "");
						
						if (tmp2 != null) {
							i2.putExtra(InfoOfMessage.MESSAGE_LIST, tmp2);
							sendBroadcast(i2);	
							Log.i("friend list broadcast sent ", "");
						}
						}
						else {
							Log.i("friend list returned null", "");
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}					
				}			
			}, UPDATE_TIME_PERIOD, UPDATE_TIME_PERIOD);
		}
		
		return result;		
	}

	public void messageReceived(String username, String message) 
	{				
		
		//FriendInfo friend = FriendController.getFriendInfo(username);
		InfoOfMessage msg = MessageController.checkMessage(username);
		if ( msg != null)
		{			
			Intent i = new Intent(TAKE_MESSAGE);
		
			i.putExtra(InfoOfMessage.USERID, msg.userid);			
			i.putExtra(InfoOfMessage.MESSAGETEXT, msg.messagetext);			
			sendBroadcast(i);
			String activeFriend = ControllerOfFriend.getActiveFriend();
			if (activeFriend == null || activeFriend.equals(username) == false) 
			{
				localstoragehandler.insert(username,this.getUsername(), message.toString());
				showNotification(username, message);
			}
			
			Log.i("TAKE_MESSAGE broadcast sent by im service", "");
		}	
		
	}  
	
	private String getAuthenticateUserParams(String usernameText, String passwordText) throws UnsupportedEncodingException 
	{			
		String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") +
						"&password="+ URLEncoder.encode(passwordText,"UTF-8") +
						"&action="  + URLEncoder.encode("authenticateUser","UTF-8")+
						"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") +
						"&";		
		
		return params;		
	}

	public void setUserKey(String value) 
	{		
	}

	public boolean isNetworkConnected() {
		return conManager.getActiveNetworkInfo().isConnected();
	}
	
	public boolean isUserAuthenticated(){
		return authenticatedUser;
	}
	
	public String getLastRawFriendList() {		
		return this.rawFriendList;
	}
	
	@Override
	public void onDestroy() {
		Log.i("IMService is being destroyed", "...");
		super.onDestroy();
	}
	
	public void exit() 
	{
		timer.cancel();
		socketOperator.exit(); 
		socketOperator = null;
		this.stopSelf();
	}
	
	public String signUpUser(String usernameText, String passwordText,
			String emailText) 
	{
		String params = "username=" + usernameText +
						"&password=" + passwordText +
						"&action=" + "signUpUser"+
						"&email=" + emailText+
						"&";
		
		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
	}

	public String addNewFriendRequest(String friendUsername) 
	{
		String params = "username=" + this.username +
		"&password=" + this.password +
		"&action=" + "addNewFriend" +
		"&friendUserName=" + friendUsername +
		"&";

		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
	}

	public String sendFriendsReqsResponse(String approvedFriendNames,
			String discardedFriendNames) 
	{
		String params = "username=" + this.username +
		"&password=" + this.password +
		"&action=" + "responseOfFriendReqs"+
		"&approvedFriends=" + approvedFriendNames +
		"&discardedFriends=" +discardedFriendNames +
		"&";

		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
		
	} 
	
	private void parseFriendInfo(String xml)
	{			
		try 
		{
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			sp.parse(new ByteArrayInputStream(xml.getBytes()), new HandlerXML(MessagingService.this));		
		} 
		catch (ParserConfigurationException e) {			
			e.printStackTrace();
		}
		catch (SAXException e) {			
			e.printStackTrace();
		} 
		catch (IOException e) {			
			e.printStackTrace();
		}	
	}
	private void parseMessageInfo(String xml)
	{			
		try 
		{
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			sp.parse(new ByteArrayInputStream(xml.getBytes()), new HandlerXML(MessagingService.this));		
		} 
		catch (ParserConfigurationException e) {			
			e.printStackTrace();
		}
		catch (SAXException e) {			
			e.printStackTrace();
		} 
		catch (IOException e) {			
			e.printStackTrace();
		}	
	}

	public void updateData(InfoOfMessage[] messages,InfoOfFriend[] friends,
			InfoOfFriend[] unApprovedFriends, String userKey) 
	{
		this.setUserKey(userKey);
		//FriendController.	
		MessageController.setMessagesInfo(messages);
		//Log.i("MESSAGEIMSERVICE","messages.length="+messages.length);
		
		int i = 0;
		while (i < messages.length){
			messageReceived(messages[i].userid,messages[i].messagetext);
			//appManager.messageReceived(messages[i].userid,messages[i].messagetext);
			i++;
		}
		
		
		ControllerOfFriend.setFriendsInfo(friends);
		ControllerOfFriend.setUnapprovedFriendsInfo(unApprovedFriends);
		
	}


	
	
	
	
}