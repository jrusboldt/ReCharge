//
//  SettingsViewController.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 2/18/19.
//

import UIKit
import os.log


class SettingsViewController: UIViewController {

    @IBOutlet weak var proximitySlider: UISlider!
    @IBOutlet weak var sliderValue: UILabel!
    @IBOutlet weak var saveButton: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        sliderValue.text = "\(Int(userSettings.proximity))"
        proximitySlider.value = Float((userSettings.proximity))
        
        /*
        if userSettings != nil {
            print("userSettings is loading.")
            sliderValue.text = "\(userSettings.proximity)"
            proximitySlider.value = Float((userSettings.proximity))
        }
        else {
            print("userSettings is nill.")
            sliderValue.text = "10"
            proximitySlider.value = 20
        }
 */
        
        proximitySlider.minimumValue = 0.1
        proximitySlider.maximumValue = 10
        
        // Do any additional setup after loading the view.
    }
    
    @IBAction func saveButtonTouched(_ sender: Any) {
        print("save button pressed")
        userSettings.proximity = Double(proximitySlider.value)
        //saveProximity()
    }
    
    
    @IBAction func sliderValueChanged(_ sender: UISlider) {
        sliderValue.text = "\(Int(sender.value))"
    }
    
    
    
    private func saveProximity() {
        let isSuccessfulSave = NSKeyedArchiver.archiveRootObject(Double(proximitySlider.value), toFile: Settings.ArchiveURL.path)
    
        if isSuccessfulSave {
            print("Settings successfully saved.")
        } else {
            print("Failed to save settings...")
        }
    }
    
    
}
