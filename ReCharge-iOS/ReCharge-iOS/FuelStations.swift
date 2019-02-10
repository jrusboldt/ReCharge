//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Tyler Emerick on 2/10/19.
//


import MapKit

class FuelStations: NSObject, MKAnnotation {
  // instance variables
  let title : String?
  let station_name : String
  let station_phone : String
  let latitude : Double
  let longitude : Double
  let city : String
  let intersection_directions : String
  let state : String
  let street_address : String
  let zip : String
  let coordinate : CLLocationCoordinate2D
  let is_parking_avaiable : Bool
  let is_charging_avaiavle : Bool

  init(JSONString: String) {
    super.init()

    var error : NSError?
    let JSONData = JSONString.dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)

    let JSONDictionary: Dictionary = NSJSONSerialization.JSONObjectWithData(JSONData, options: nil, error: &error) as NSDictionary

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

  }

  //init()

}
