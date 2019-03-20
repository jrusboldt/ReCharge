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
    
    @IBOutlet weak var availableSwitch: UISwitch!
    @IBOutlet weak var busySwitch: UISwitch!
    @IBOutlet weak var freeSwitch: UISwitch!
    @IBOutlet weak var paidSwitch: UISwitch!
    @IBOutlet weak var standardSwitch: UISwitch!
    @IBOutlet weak var fastSwitch: UISwitch!
    
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
    
    @IBAction func availableSwitchTouched(_ sender: Any) {
        //get current switch value
        let value = availableSwitch.isOn
        
        print("available switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.availableToggle = value
        //flip switch
        availableSwitch.setOn(value, animated: true)
    }
    
    @IBAction func busySwitchTouched(_ sender: Any) {
        //get current switch value
        let value = busySwitch.isOn
        
        print("busy switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.busyToggle = value
        //flip switch
        busySwitch.setOn(value, animated: true)
    }
    
    @IBAction func freeSwitchTouched(_ sender: Any) {
        //get current switch value
        let value = freeSwitch.isOn
        
        print("free switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.freeToggle = value
        //flip switch
        freeSwitch.setOn(value, animated: true)
    }
    
    @IBAction func paidSwitchTouched(_ sender: Any) {
        //get current switch value
        let value = paidSwitch.isOn
        
        print("paid switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.paidToggle = value
        //flip switch
        paidSwitch.setOn(value, animated: true)
    }
    
    @IBAction func standardSwitchTouched(_ sender: Any) {
        //get current switch value
        let value = standardSwitch.isOn
        
        print("standard switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.standardToggle = value
        //flip switch
        standardSwitch.setOn(value, animated: true)
    }
    
    @IBAction func fastSwitchTouched(_ sender: Any) {
        //get current switch value
        let value = fastSwitch.isOn
        
        print("fast switch pressed, value: \(value)")
        
        //save new value in user settings
        userSettings.fastToggle = value
        //flip switch
        fastSwitch.setOn(value, animated: true)
    }
    
    
}
