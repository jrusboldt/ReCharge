//
//  Settings.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 2/21/19.
//

import Foundation
import os.log

class Settings : NSObject, NSCoding{
    
    var proximity : Double
    
    init(proximity: Double) {
        self.proximity = proximity
    }
    
    struct PropertyKey {
        static let proximity = "proximity"
    }
    
    func encode(with aCoder: NSCoder) {
        aCoder.encode(proximity, forKey: PropertyKey.proximity)
    }
    
    required convenience init?(coder aDecoder: NSCoder) {
        // The name is required. If we cannot decode a name string, the initializer should fail.
        guard let proximity = aDecoder.decodeObject(forKey: PropertyKey.proximity) as? Double else {
            os_log("Unable to decode the name for a Meal object.", log: OSLog.default, type: .debug)
            return nil
        }
        
        // Must call designated initializer
        self.init(proximity: proximity)
    }
    
    //MARK: Archiving Paths
    
    static let DocumentsDirectory = FileManager().urls(for: .documentDirectory, in: .userDomainMask).first!
    static let ArchiveURL = DocumentsDirectory.appendingPathComponent("settings")
    
    
    
    
}
