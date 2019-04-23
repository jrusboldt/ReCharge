//Initiallising node modules
var express = require("express");
var bodyParser = require("body-parser");
var sql = require("mssql");
var app = express();

app.use(bodyParser.urlencoded({
  extended: true
}));

var server = app.listen(process.env.PORT || 8080, function () {
    var port = server.address().port;
    console.log("[" + getFormattedDate() + "] " + "App now running on port", port);
 });

var mysql = require('mysql');

var connection = mysql.createConnection({
  host     : "recharge.c87i64zdxk4i.us-east-2.rds.amazonaws.com",
  user     : "*****",
  password : "*****",
  port     : 3306
});

connection.connect(function(err) {
  if (err) {
    console.error("[" + getFormattedDate() + "] " + 'Database connection failed: ' + err.stack);
    return;
  }
  console.log("[" + getFormattedDate() + "] " + 'Connected to database.');
});

setInterval(emulateSensor, 600000);

var  executeQuery = function(res, query){
  connection.query(query, function (error, results, fields) {
		if (error) {
      res.send(JSON.stringify({"status": 100, "error": error, "response": null}));
      console.log("[" + getFormattedDate() + "] " + error);
    } else {
		res.send(JSON.stringify({"status": 200, "error": null, "response": results}));
  }
  });
}

app.get("/api/description/all", function(req , res){
                var query = "SELECT * FROM main.DESCRIPTION";
                executeQuery(res, query);
});

 app.post("/api/description/specific", function(req , res){
                var query = "SELECT * FROM main.DESCRIPTION WHERE ID =" + req.body.ID;
                executeQuery(res, query);
});

app.get("/api/status/all", function(req , res){
                var query = "SELECT * FROM main.STATUS";
                executeQuery(res, query);
});

 app.post("/api/status/specific", function(req , res){
                var query = "SELECT * FROM main.STATUS WHERE ID =" + req.body.ID;
                executeQuery(res, query);
});

 app.post("/api/history/specific", function(req , res){
                var query = "SELECT * FROM main.HISTORY WHERE ID =" + req.body.ID;
                executeQuery(res, query);
});

function emulateSensor() {
    var parkingSpaceID = 76232;
    var firstParkingSpace = Math.floor(Math.random()*2);
    var secondParkingSpace = Math.floor(Math.random()*2);
    var remainUnchanged = Math.floor(Math.random()*4);
    var usingCharger = 0;

    if (usingCharger == 0 && firstParkingSpace == 1) {
      usingCharger = Math.floor(Math.random()*2);
    }

    if (usingCharger == 0 && secondParkingSpace == 1) {
      usingCharger = Math.floor(Math.random()*2);
    }

    if (usingCharger == 1) {
      usingCharger = "Y";
    } else {
      usingCharger = "N";
    }

    //ex.
    //parkingSpaceID = 76232;
    //usingCharger = Y;
    //firstParkingSpace = 0
    //secondParkingSPace = 1
    //remainUnchanged = 0

    var insertQuery = "INSERT INTO main.HISTORY (ID, TIMESTAMP, AVAILABILITY) VALUES (" + parkingSpaceID + ", CURRENT_TIMESTAMP , CASE WHEN (SELECT AVAILABLE FROM main.STATUS WHERE ID = " + parkingSpaceID + ") = \"Y\" THEN \"AVAILABLE\" ELSE \"IN-USE\" END )";
    var updateQuery = "UPDATE main.STATUS SET AVAILABLE = \"" + usingCharger + "\", REMAINING_SPACE = " + (firstParkingSpace + secondParkingSpace) + " WHERE ID = " + parkingSpaceID;

    connection.query(insertQuery, function (error, results, fields) {
      if (error) {
        console.log("[" + getFormattedDate() + "] " + "EMULATION INSERTION ERROR: " + error);
      } else {
        console.log("[" + getFormattedDate() + "] " + "EMULATION INSERTION SUCCESS");
        if (remainUnchanged == 0) {
          console.log("[" + getFormattedDate() + "] " + "EMULATION UPDATE SUCCESS | (SKIPPED)");
          console.log("[" + getFormattedDate() + "] " + "Waiting...");
        } else {
          connection.query(updateQuery, function (error, results, fields) {
            if (error) {
              console.log("[" + getFormattedDate() + "] " + "EMULATION UPDATE ERROR: " + error);
            } else {
              console.log("[" + getFormattedDate() + "] " + "EMULATION UPDATE SUCCESS | Using Charger: " + usingCharger + " | Parking Spaces: " + (firstParkingSpace + secondParkingSpace));
              console.log("[" + getFormattedDate() + "] " + "Waiting...");
            }
          });
        }
      }
    });
}

//Credit goes to StackOverflow (https://stackoverflow.com/questions/926332/how-to-get-formatted-date-time-like-2009-05-29-215557-using-javascript)
function getFormattedDate() {
    var date = new Date();
    var str = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate() + " " +  date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
    return str;
}
