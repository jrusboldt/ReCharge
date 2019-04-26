//
//  StationHistory.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 4/25/19.
//

import Foundation

struct AWSJsonObj2: Decodable {
    
    // JSON object from the AWS database query
    let status : Int
    let error : String?
    //array of stations' history
    let response: [StationHistory]
}

struct StationHistory: Decodable {
    
    let ID : Int
    let TIMESTAMP : String?
    let AVAILABLE : String?
}
