# Androidchat
Android chat app


Features
-----------------------------------------------
   1)  Registering User
   2)  Authenticating User
   3)  New Friend Adding via User name
   4)  Approving Friend
   5)  Messaging with a friend in list
   6)  Shows online and offline users
   7)  Uses notification area when a new message is received.
   8)  Quitting the application(kills the background service) 



Installation Method Must Read it 
-----------------------------------------------
    Step 1:  Web Application Programming Interface
   
        A folder named as "AndroidChatterDatabase", just copy it to your webserver directory , in Xamp Server it is htdocs folder.
        AndroidChatterDatabase that can be accessed by http://YOUR_COMPUTER_IP_ADDRESS/AndroidChatterDatabase/ is the IP address of 
        computer which runs apache and mysql. 
		Note : Use local network IP address instead of using localhost or 127.0.0.1).
        Open the index.php and enter database connectivity parameters such as host, username, password etc.  
        Create the tables in mysql database with the included android_im.sql
        
    Step 2: Android APP
   
        set AUTHENTICATION_SERVER_ADDRESS in socketOperator, it must be the address where server folder are located,
        for our example it is  http://YOUR_COMPUTER_IP_ADDRESS/AndroidChatterDatabase/         (Don't use Localhost)
        
    
	
    Step 3: Then run your application in Eclipse with ADT plugin.
    

	
	Note : You can get your computer local IP Address using command prompt by typing the following command "ipconfig"
		   Your computer when connected to Internet changes it's IP frequently so make sure , whenever you're running
		   AndroidChatter Application you check out and update Socketer Class with your Computer IP address.
		   
		   
		   If you want to check this application online , Then first of all buy a domain and grab the ip address of that domain
		   then upload the databases file to that domain and put the IP address of that domain in Socketer Class.
		   
		   
		   
	
	
	
	
