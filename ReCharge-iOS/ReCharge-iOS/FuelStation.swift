//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Tyler Emerick on 2/10/19.
//


import MapKit
import Contacts

class FuelStationAnnotation: NSObject, MKAnnotation {
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
    let is_paid: Bool

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

    init(stationName: String, streetAddress: String, isParkingAvaiable : Bool, isChargingAvaiable : Bool, isPaid: Bool, coordinate: CLLocationCoordinate2D) {
        self.station_name = stationName
        self.street_address = streetAddress
        self.is_parking_avaiable = isParkingAvaiable
        self.is_charging_avaiable = isChargingAvaiable
        self.is_paid = isPaid
        self.coordinate = coordinate
        
        super.init()
    }
    
    init(station_name: String, is_paid: Bool, latitude: Double, longitude: Double) {
        self.station_name = station_name
        self.is_paid = is_paid
        self.street_address = "Not Avaiable"
        self.is_parking_avaiable = false
        self.is_charging_avaiable = false
        self.coordinate = CLLocationCoordinate2DMake(latitude, longitude)
        
        super.init()
    }
    
    var title: String? {
        return station_name
    }
    
    var subtitle: String? {
        return street_address
    }
    
    // Annotation right callout accessory opens this mapItem in Maps app
    func mapItem() -> MKMapItem {
        let addressDict = [CNPostalAddressStreetKey: subtitle!]
        let placemark = MKPlacemark(coordinate: coordinate, addressDictionary: addressDict)
        let mapItem = MKMapItem(placemark: placemark)
        mapItem.name = title
        return mapItem
    }

}
