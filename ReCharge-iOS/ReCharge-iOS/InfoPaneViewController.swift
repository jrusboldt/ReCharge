//
//  InfoPaneViewController.swift
//  ReCharge-iOS
//
//  Created by csuser on 2/16/19.
//

import UIKit


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
    
    var delegate : InfoPaneDelegateProtocol?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.layer.cornerRadius = 5;
        view.layer.masksToBounds = true;
        // Do any additional setup after loading the view.
    }
    
    func showInfoPane (){
        delegate?.openInfoPane()
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
