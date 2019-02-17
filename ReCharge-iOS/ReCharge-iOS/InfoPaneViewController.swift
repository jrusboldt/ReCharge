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
    
    var delegate : InfoPaneDelegateProtocol?
    
    override func viewDidLoad() {
        super.viewDidLoad()

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
