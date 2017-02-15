package xyz.donot.detector.model

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass
open class UserObject : RealmObject() {
    open  var user: ByteArray? =null
}