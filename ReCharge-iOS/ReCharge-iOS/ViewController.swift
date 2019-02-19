//
//  ViewController.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 2/5/19.
//

import UIKit
import MapKit
import CoreLocation

class ChargingStationAnnotation: NSObject, MKAnnotation {
    
    var coordinate: CLLocationCoordinate2D
    let title: String?
    
    
    init(coordinate: CLLocationCoordinate2D, title: String) {
        self.title = title
        self.coordinate = coordinate
        super.init()
    }
}

extension ViewController: MKMapViewDelegate {
    // 1
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        // 2
        guard let annotation = annotation as? FuelStationAnnotation else { return nil }
        // 3
        let identifier = "marker"
        var view: MKMarkerAnnotationView
        // 4
        if let dequeuedView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
            as? MKMarkerAnnotationView {
            dequeuedView.annotation = annotation
            view = dequeuedView
        } else {
            // 5
            view = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.canShowCallout = true
            view.calloutOffset = CGPoint(x: -5, y: 5)
            view.rightCalloutAccessoryView = UIButton(type: .detailDisclosure)
        }
        return view
    }
}


class ViewController: UIViewController {

    @IBOutlet weak var mapView: MKMapView!
    

    
    let locationManager = CLLocationManager()
    let regionInMeters: Double = 1000
    
    var stations = [FuelStationAnnotation]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        checkLocationServices()
    }
    
    func getNREL(coordinate: CLLocationCoordinate2D, amount: Int) {
        
        let scriptUrl = URL(string: "https://developer.nrel.gov/api/alt-fuel-stations/v1/nearest.json?api_key=OxpIDL7uE8O60BL52DC7YYp3T1mq4uy01wlLw5bK&latitude=\(coordinate.latitude)&longitude=\(coordinate.longitude)&limit=10")!
    
        let configuration = URLSessionConfiguration.ephemeral
        let session = URLSession(configuration: configuration, delegate: nil, delegateQueue: OperationQueue.main)
        let task = session.dataTask(with: scriptUrl, completionHandler: { [weak self] (data: Data?, response: URLResponse?, error: Error?) -> Void in
            // Parse the data in the response and use it
            do {
                let json = try JSONSerialization.jsonObject(with: data!, options: []) as! [String: AnyObject]
                
                print(json)
                
                //let stationArray = json["fuel_stations"]
                //print(stationArray)
                
                //TODO figure out how to parse JSON object
                
                //hardcode in station data for West Lafette
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Armory", latitude: 40.4277617, longitude: -86.9162607))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Northwestern Parking Garage", latitude: 40.4296753, longitude: -86.9120266))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - University Street Garage", latitude: 40.426713, longitude: -86.917213))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Grant Street Parking Garage", latitude: 40.4244203, longitude: -86.9103211))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Harrison Street Garage", latitude: 40.421241, longitude: -86.917619))
                
                self?.addStationAnnotations()
            } catch let error as NSError {
                print("Failed to load: \(error.localizedDescription)")
            }
        })
        task.resume()
        
    }
    
    //adds map annotations using array of stations pulled from NREL database
    func addStationAnnotations() {
        for i in stations {
            mapView.addAnnotation(i)
        }
    }
    
    private func registerMapAnnotationViews() {
        mapView.register(MKAnnotationView.self, forAnnotationViewWithReuseIdentifier: NSStringFromClass(ChargingStationAnnotation.self))
    }

    func setupLocationManager() {
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }
    
    func centerViewOnUserLocation() {
        if let location = locationManager.location?.coordinate {
            let region = MKCoordinateRegion.init(center: location, latitudinalMeters: regionInMeters, longitudinalMeters: regionInMeters)
            mapView.setRegion(region, animated: true)
            
            getNREL(coordinate: location, amount: 5)
            
        }
    }

    //check system wide location services
    func checkLocationServices() {
        if CLLocationManager.locationServicesEnabled() {
            setupLocationManager()
            checkLocationAuthorization()
            locationManager.startUpdatingLocation()
        }
        else {
            // show alert to enable location services
        }
    }
    
    //checks app specific location services
    func checkLocationAuthorization() {
        switch CLLocationManager.authorizationStatus() {
        case .authorizedWhenInUse:
            mapView.showsUserLocation = true
            centerViewOnUserLocation()
            break
        case .denied:
            //show alert to turn on location services in settings
            break
        case .notDetermined:
            //prompt user to allow location services when app in use
            locationManager.requestWhenInUseAuthorization()
            break
        case .restricted:
            //location services are unavialable due to parental controls
            //show alert
            break
        case .authorizedAlways:
            break
        default:
            break
        }
    }
}

extension ViewController: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        let center = CLLocationCoordinate2D(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
        let region = MKCoordinateRegion.init(center: center, latitudinalMeters: regionInMeters, longitudinalMeters: regionInMeters)
        mapView.setRegion(region, animated: true)
    }
    
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        checkLocationAuthorization()
    }
}

