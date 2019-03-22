# ReCharge
Re:Charge - Team 24's project for CS 40700

## API Server Information

**API Endpoint:** 18.224.1.103:8080

*POST* APIs:

  * /api/description/specific
  * /api/history/specific
  * /api/status/specific

   For all POST APIs:

    { ID: 1 }

*GET* APIs:

  * /api/description/all
  * /api/history/all
  * /api/status/all


Example:

*Input:*
GET http://18.224.1.103:8080/api/status/all

*Output:*
{"status":200,"error":null,"response":[{"ID":1,"AVAILABLE":"Y"}]}


###### Note: There is no difference b/w POST & GET APIs for demo purposes as there is only one charging station.
