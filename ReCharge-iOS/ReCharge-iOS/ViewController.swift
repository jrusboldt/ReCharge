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
        guard let annotation = annotation as? ChargingStationAnnotation else { return nil }
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
    
    func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView){
        if let embeddedViewController = children.first as? InfoPaneViewController,
            let annotation = view.annotation,
            let title = annotation.title {
            embeddedViewController.stationName.text = title
            embeddedViewController.showInfoPane()
        }
        
    }
    
}

class ViewController: UIViewController, InfoPaneDelegateProtocol {
    
    @IBOutlet weak var mapView: MKMapView!
    @IBOutlet weak var containerView: UIView!
    
    
    let locationManager = CLLocationManager()
    let regionInMeters: Double = 10000
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(containerView)
        self.closeInfoPane()
        checkLocationServices()
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        //check here for the right segue by name
        (segue.destination as! InfoPaneViewController).delegate = self;
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
            
            
            let annotation = ChargingStationAnnotation(coordinate: CLLocationCoordinate2D(latitude: location.latitude, longitude: location.longitude), title: "Station 1")
            
            mapView.addAnnotation(annotation)
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

