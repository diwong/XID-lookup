package XID

import java.util.UUID

//case class EntryRecord(val tenant: String, val SSId:  String, val XId: UUID, val Id: UUID)
case class EntryRecord(tenant: String, SSId:  String, XId: String, Id: String)

object model {
  //var entries : List[EntryRecord] = List()
  var entries : List[EntryRecord] = List(EntryRecord("test", "S1", "X1", "I1"), EntryRecord("test", "S2", "X2", "I2"))

  //def Add(tenant: String, SSId: String, Id: UUID): EntryRecord = {
  def Add(tenant: String, SSId: String, Id: String): EntryRecord = {
//    val XId = UUID.randomUUID() //generate XID
    val XId = "X3" // hard-coded for testing purpose
    val entry = EntryRecord(tenant, SSId, XId.toString(), Id)
    entries = List.concat(entries, List(entry))
    entry
  }

  //def FindId(tenant: String, XId: UUID): EntryRecord = {
  def FindId(tenant: String, XId: String): EntryRecord = {
    //TODO: need to handle error condition where there is no match
    entries.filter(e => (e.tenant == tenant && e.XId == XId)).head
  }

  def FindXId(tenant: String, SSId: String): EntryRecord = {
    //TODO: need to handle error condition where there is no match
    entries.filter(e => (e.tenant == tenant && e.SSId == SSId)).head
  }
}
