package com.`in`.videoassignment.data

import com.google.android.gms.common.api.Response
import java.io.Serializable

class Response : Serializable
{
    var status: Status
    var message:String
    var Response:Any?

    constructor(status: Status,message:String,response:Any?)
    {
        this.status = status
        this.message=message
        this.Response=response
    }
}
