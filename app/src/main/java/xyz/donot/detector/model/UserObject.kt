package xyz.donot.detector.model

import io.realm.RealmObject


open class UserObject : RealmObject() {
    open  var user: ByteArray? =null
    open var id:Long=0L
}