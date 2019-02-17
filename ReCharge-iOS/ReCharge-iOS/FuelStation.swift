//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Tyler Emerick on 2/10/19.
//


import MapKit

class FuelStation: NSObject, MKAnnotation {
    // instance variables (some commented out for initial testing)
    let station_name : String
    //let station_phone : String?
    //let latitude : Double
    //let longitude : Double
    //let city : String
    //let intersection_directions : String
    //let state : String
    let street_address : String
    //let zip : String
    let coordinate : CLLocationCoordinate2D
    let is_parking_avaiable : Bool
    let is_charging_avaiable : Bool

    /*
    init(JSONString: String) {
    super.init()

        var error : NSError?
        let JSONData = JSONString.data(using: String.Encoding.utf8, allowLossyConversion: false)

        let JSONDictionary: Dictionary = JSONSerialization.JSONObjectWithData(JSONData, options: nil) as NSDictionary

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
    */

    init(stationName: String, streetAddress: String, isParkingAvaiable : Bool, isChargingAvaiable : Bool, coordinate: CLLocationCoordinate2D) {
        self.station_name = stationName
        self.street_address = streetAddress
        self.is_parking_avaiable = isParkingAvaiable
        self.is_charging_avaiable = isChargingAvaiable
        self.coordinate = coordinate
        
        super.init()
    }
    
    var title: String? {
        return station_name
    }
    
    var subtitle: String? {
        return street_address
    }

}
