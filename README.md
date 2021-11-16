#Thingsboard Data Query REST API  
  https://thingsboard.io/docs/user-guide/telemetry/  

  Add okhttp dependencies : https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp  
  Add gson dependencies : https://mvnrepository.com/artifact/com.google.code.gson/gson  

#ThingsBoard REST API  

  Refer to https://thingsboard.io/docs/reference/rest-api/  
      get the JSON Web Token(JWT)  
      $ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{"username":"username", "password":"password"}' 'http://THINGSBOARD_URL/api/auth/login'  
        >> display TOKEN  
  
      Data Query REST API : https://thingsboard.io/docs/user-guide/telemetry/  
      $ curl -v -X GET http://THINGSBOARD_URL/api/plugins/telemetry/DEVICE/$DEVICE_ID/keys/timeseries --header "Content-Type:application/json" --header "X-Authorization: Bearer $JWT_TOKEN"  
        
      Note : Online decode JWT : https://jwt.io/


