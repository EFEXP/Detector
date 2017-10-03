package xyz.donot.detector.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey



open class StatusObject : RealmObject() {
    open  var status: ByteArray? =null
    open  var picture: ByteArray? =null
    @PrimaryKey open  var statusId: Long =0L
}
