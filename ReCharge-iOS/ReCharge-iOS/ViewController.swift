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
            //view.rightCalloutAccessoryView = UIButton(type: .detailDisclosure)
            
            if (annotation.is_paid){
                view.glyphText = "$"
            }
        }
        return view
    }
    
    // loads data into info
    func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView){
        if let embeddedViewController = children.first as? InfoPaneViewController,
            let annotation = view.annotation,
            let fuelStation = annotation as? FuelStationAnnotation {
            
            embeddedViewController.annotation = fuelStation
            /*
            embeddedViewController.stationName.text = fuelStation.station_name
            embeddedViewController.streetAddress.text = fuelStation.street_address
            if (fuelStation.is_parking_avaiable){
                embeddedViewController.isParkingAvaiable.text = "Yes"
            } else {
                embeddedViewController.isParkingAvaiable.text = "No"
            }
            if (fuelStation.is_charging_avaiable){
                embeddedViewController.isChargingAvaiable.text = "Yes"
            } else {
                embeddedViewController.isChargingAvaiable.text = "No"
            }*/
            embeddedViewController.populateInfoPane(fuelStation: fuelStation)
            embeddedViewController.showInfoPane()
        }
        
    }
    
}

var userSettings : Settings = Settings(proximity: 3)

class ViewController: UIViewController, InfoPaneDelegateProtocol {
    
    @IBOutlet weak var mapView: MKMapView!
    @IBOutlet weak var containerView: UIView!
    
    
    let locationManager = CLLocationManager()
    let regionInMeters: Double = 500
    var firstLoad: Bool = true
    
    var stations = [FuelStationAnnotation]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(containerView)
        self.closeInfoPane()
        checkLocationServices()
    
        //userSettings = loadSettings()!
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // check for segue name
        if (segue.identifier == "InfoPane") {
            // set self as InfoPaneVC.delegate
            (segue.destination as! InfoPaneViewController).delegate = self;
        }
        
    }
    // InfoPane functions
    func openInfoPane() {
        containerView.isHidden = false
    }
    
    func closeInfoPane() {
        containerView.isHidden = true
    }
    
    @IBAction func unwindToMapView(_ sender: UIStoryboardSegue) {
        self.closeInfoPane()
    }
    
    // map function
    /*
    private func loadSettings() -> Settings? {
        return NSKeyedUnarchiver.unarchiveObject(withFile: Settings.ArchiveURL.path) as? Settings
    }
 */
    
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
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Armory", is_paid: false, latitude: 40.4277617, longitude: -86.9162607))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Northwestern Parking Garage", is_paid: false, latitude: 40.4296753, longitude: -86.9120266))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - University Street Garage", is_paid: true, latitude: 40.426713, longitude: -86.917213))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Grant Street Parking Garage", is_paid: false, latitude: 40.4244203, longitude: -86.9103211))
                self!.stations.append(FuelStationAnnotation.init(station_name: "Purdue University - Harrison Street Garage", is_paid: true, latitude: 40.421241, longitude: -86.917619))
                
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
            var region = MKCoordinateRegion.init(center: location, latitudinalMeters: Double(userSettings.proximity*regionInMeters),
                                                 longitudinalMeters: Double(userSettings.proximity*1500))
            
            mapView.setRegion(region, animated: true)
            
            getNREL(coordinate: location, amount: 5)
        }
    }

    private func displaySystemLocationWarning(){
        let alert = UIAlertController(title: "Location Services Permission Alert", message: "You need to give location permission to Re:Charge to experience all the features of the Re:Charge application.", preferredStyle: .alert)
        
        alert.addAction(UIAlertAction(title: "Go to Settings", style: .cancel, handler:{ action in
            UIApplication.shared.open(URL(string:"App-Prefs:root=Privacy&path=LOCATION")!)
            
        }))
        alert.addAction(UIAlertAction(title: "Ignore", style: .default, handler: nil))
        
        
        self.present(alert, animated: true)
    }
    
    //check system wide location services
    func checkLocationServices() {
        if CLLocationManager.locationServicesEnabled() {
            setupLocationManager()
            checkLocationAuthorization()
            locationManager.startUpdatingLocation()
            mapView.userTrackingMode = MKUserTrackingMode(rawValue: 2)!
        }
        else {
            // show alert to enable location services
            displaySystemLocationWarning()
        }
    }
    
    private func displayLocationWarning(){
        let alert = UIAlertController(title: "Location Services Permission Alert", message: "You need to give location permission to Re:Charge to experience all the features of the Re:Charge application.", preferredStyle: .alert)
        
        alert.addAction(UIAlertAction(title: "Go to Settings", style: .cancel, handler:{ action in
            UIApplication.shared.open(URL(string:UIApplication.openSettingsURLString)!)
            
        }))
        alert.addAction(UIAlertAction(title: "Ignore", style: .default, handler: nil))
        
        
        self.present(alert, animated: true)
    }
    
    //checks app specific location services
    func checkLocationAuthorization() {
        switch CLLocationManager.authorizationStatus() {
        case .authorizedWhenInUse:
            mapView.showsUserLocation = true
            centerViewOnUserLocation()
            break
        case .denied:
            displayLocationWarning()
            break
        case .notDetermined:
            //prompt user to allow location services when app in use
            locationManager.requestWhenInUseAuthorization()
            break
        case .restricted:
            //location services are unavialable due to parental controls
            //show alert
            displayLocationWarning()
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
        let region = MKCoordinateRegion(center: center, span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01))
        //self.mapView.setRegion(region, animated: true)
    }
    
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        checkLocationAuthorization()
    }
}

