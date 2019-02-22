//
//  SettingsViewController.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 2/18/19.
//

import UIKit


class SettingsViewController: UIViewController {

    @IBOutlet weak var proximitySlider: UISlider!
    @IBOutlet weak var sliderValue: UILabel!
    
    @IBAction func sliderValueChanged(_ sender: UISlider) {
        sliderValue.text = "\(sender.value)"
    }
    
    
    
    override func viewDidLoad() {
        sliderValue.text = "\(proximitySlider.value)"
        proximitySlider.minimumValue = 0
        proximitySlider.maximumValue = 100
        super.viewDidLoad()

        // Do any additional setup after loading the view.
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
