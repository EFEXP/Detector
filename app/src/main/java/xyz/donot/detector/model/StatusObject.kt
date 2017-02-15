package xyz.donot.detector.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass


@RealmClass
open class StatusObject : RealmObject() {
    open  var status: ByteArray? =null
    @PrimaryKey open  var statusId: Long =0L
}
