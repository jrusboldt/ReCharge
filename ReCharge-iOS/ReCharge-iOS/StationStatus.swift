//
//  StationStatus.swift
//  ReCharge-iOS
//
//  Created by Justin Boudreau on 4/23/19.
//

import Foundation

struct AWSJsonObj: Decodable {
    
    // JSON object from the AWS database query
    let status : Int
    let error : String?
    //array of stations' status
    let response: [StationStatus]
}

struct StationStatus: Decodable {
    
    let ID : Int
    let AVAILABLE : String?
    let REMAINING_SPACE : Int
}


