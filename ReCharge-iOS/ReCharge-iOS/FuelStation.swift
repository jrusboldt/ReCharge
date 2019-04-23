//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Tyler Emerick on 2/10/19.
//


import MapKit
import Contacts

struct NRELJsonObj: Decodable {
    
    // JSON object from the NREL database querry
    let latitude : Double
    let longitude : Double
    let location_country: String?
    let percision : String?
    let station_locator_url: String
    let total_results: Int
    // station_counts json obj not constructed
    let offset : Int
    let fuel_stations: [NRELFuelStation]
}

struct NRELFuelStation: Decodable {
    // struct to seralize fuel_stations from the NRELJsonObj
    let access_code: String?
    let access_days_time: String?
    let access_detail_code: String?
    let cards_accepted: String?
    let date_last_confirmed: String?
    let expected_date: String?
    let fuel_type_code: String?
    let groups_with_access_code: String?
    let id: Int
    let open_date: String?
    let owner_type_code: String?
    let status_code: String?
    let station_name : String?
    let station_phone : String?
    let updated_at: String?
    let geocode_status: String?
    let latitude : Double
    let longitude : Double
    let city : String
    let intersection_directions : String?
    let state : String
    let street_address : String
    let zip : String
    let country : String
    let ev_level1_evse_num : Int?
    let ev_level2_evse_num : Int?
    let ev_dc_fast_num : Int?
}

class FuelStationAnnotation: NSObject, MKAnnotation {
    // instance variables (some commented out for initial testing)
    var stationName : String?
    var stationPhone : String?
    var city : String
    var intersectionDirections : String?
    var state : String
    var streetAddress : String
    var zip : String
    var coordinate : CLLocationCoordinate2D
    var isParkingAvaiable : Bool
    var isChargingAvaiable : Bool
    var isPaid : Bool
    var isStandardCharger : Bool
    var isDCFastCharger : Bool
    // station working status
    var isOpen : Bool
    
    init(obj: NRELFuelStation){
        
        self.stationName = obj.station_name
        self.stationPhone = obj.station_phone
        self.city = obj.city
        self.intersectionDirections = obj.intersection_directions
        self.state = obj.state
        self.streetAddress = obj.street_address + "\n" + obj.city + ", " + obj.state + "  " + obj.zip
        self.zip = obj.zip
        self.coordinate = CLLocationCoordinate2DMake(obj.latitude, obj.longitude)
        
        // TODO: get data from Ramsey's database
        
        self.isParkingAvaiable = false
        self.isChargingAvaiable = false
        //self.isPaid = obj.cards_accepted == nil ? false : true
        self.isPaid = true
        
        // check status code
        if obj.status_code == "E" {
            self.isOpen = true
        }
        else {
            self.isOpen = false
        }
        
        
        // check if lvl 1 or lvl 2 chargers are at the station
        if (obj.ev_level1_evse_num ?? 0) > 0 || (obj.ev_level2_evse_num ?? 0) > 0 {
            self.isStandardCharger = true
        }
        else {
            self.isStandardCharger = false
        }
        
        // check if DC chargers are at the station
        if (obj.ev_dc_fast_num ?? 0) > 0 {
            self.isDCFastCharger = true
        }
        else {
            self.isDCFastCharger = false
        }
        
    }
    
    var title: String? {
        return stationName
    }
    
    var subtitle: String? {
        return streetAddress
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

class FuelStationAnnotationView: MKAnnotationView {
    override var annotation: MKAnnotation? {
        willSet {
            guard let fuel_station = newValue as? FuelStationAnnotation else {return}
            
            if !fuel_station.isOpen {
                image = UIImage(named: "location-pin-not-in-service")
            }
            else {
                if fuel_station.isPaid{
                    if fuel_station.isChargingAvaiable && fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-paid-avaiable")
                    }
                    else if !fuel_station.isChargingAvaiable && fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-paid-no-charging")
                    }
                    else if fuel_station.isChargingAvaiable && !fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-paid-no-parking")
                    }
                    else{
                        image = UIImage(named: "location-pin-paid-no-charging&parking")
                    }
                }
                else {
                    if fuel_station.isChargingAvaiable && fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-avaiable")
                    }
                    else if !fuel_station.isChargingAvaiable && fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-no-charging")
                    }
                    else if fuel_station.isChargingAvaiable && !fuel_station.isParkingAvaiable{
                        image = UIImage(named: "location-pin-no-parking")
                    }
                    else{
                        image = UIImage(named: "location-pin-no-charging&parking")
                    }
                }
            }
        }
    }
}
