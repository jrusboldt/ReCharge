//
//  InfoPaneViewController.swift
//  ReCharge-iOS
//
//  Created by csuser on 2/16/19.
//

import UIKit
import MapKit
import CoreLocation
import Contacts

protocol InfoPaneDelegateProtocol
{
    func openInfoPane()
    func closeInfoPane()
}

class InfoPaneViewController: UIViewController {
    
    @IBOutlet weak var stationName: UILabel!
    @IBOutlet weak var streetAddress: UILabel!
    @IBOutlet weak var isParkingAvaiable: UILabel!
    @IBOutlet weak var isChargingAvaiable: UILabel!
    
    var annotation: FuelStationAnnotation?
    var delegate : InfoPaneDelegateProtocol?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.view.layer.cornerRadius = 5;
        self.view.layer.masksToBounds = true;
        // Do any additional setup after loading the view.
    }
    
    func showInfoPane (){
        delegate?.openInfoPane()
    }
    
    func populateInfoPane(fuelStation: FuelStationAnnotation){
        self.stationName.text = fuelStation.station_name
        self.streetAddress.text = fuelStation.street_address
        if (fuelStation.is_parking_avaiable){
            self.isParkingAvaiable.text = "Yes"
        } else {
            self.isParkingAvaiable.text = "No"
        }
        if (fuelStation.is_charging_avaiable){
            self.isChargingAvaiable.text = "Yes"
        } else {
            self.isChargingAvaiable.text = "No"
        }
    }
    
    @IBAction func loadNavigationApp(_ sender: Any) {
        let location = annotation
        let launchOptions = [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving]
        location?.mapItem().openInMaps(launchOptions: launchOptions)
    }
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
