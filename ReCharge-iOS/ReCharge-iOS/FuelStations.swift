//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Tyler Emerick on 2/10/19.
//



import MapKit

class FuelStationAnnotation: NSObject, MKAnnotation {
  // instance variables
  let title : String?
  var station_name : String
  //var station_phone : String
  var latitude : Double
  var longitude : Double
  //var city : String
  //var intersection_directions : String
  //var state : String
  //var street_address : String
  //var zip : String
  var coordinate : CLLocationCoordinate2D
  var is_parking_avaiable : Bool
  var is_charging_avaiavle : Bool

    init(station_name: String, latitude: Double, longitude: Double) {
        self.title = station_name
        self.station_name = station_name
        self.latitude = latitude
        self.longitude = longitude
        self.coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        
        self.is_parking_avaiable = true
        self.is_charging_avaiavle = true
        
        super.init()


        /*
        var error : NSError?
        let JSONData = JSONString.data(using: String.Encoding.utf8, allowLossyConversion: false)

        //let JSONDictionary: Dictionary = JSONSerialization.JSONObjectWithData(JSONData, options: nil) as NSDictionary

        // Loop
        for (key, value) in JSONDictionary {
            let keyName = key as String
            let keyValue: String = value as String

            // If property exists
            if (self.respondsToSelector(NSSelectorFromString(keyName))) {
                self.setValue(keyValue, forKey: keyName)
            }
        }

        self.title = station_name
        self.coordinate = CLLocationCoordinate2D(latitude: self.latitude, longitude: self.longitude)
    */
  }

  //init()

}

