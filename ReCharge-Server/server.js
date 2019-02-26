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
    console.log("App now running on port", port);
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
    console.error('Database connection failed: ' + err.stack);
    return;
  }
  console.log('Connected to database.');
});

var  executeQuery = function(res, query){
  connection.query(query, function (error, results, fields) {
		if (error) {
      res.send(JSON.stringify({"status": 100, "error": error, "response": null}));
      console.log(error);
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
