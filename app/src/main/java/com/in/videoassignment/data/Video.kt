package com.`in`.videoassignment.data

import java.io.Serializable


class Video :Serializable{
  var id:String?
  var VideoName:String?
  var VideoUrl:String?

  constructor(id: String?, VideoName: String?, VideoUrl:String?)
  {
    this.id=id
    this.VideoName=VideoName
    this.VideoUrl=VideoUrl
  }
}