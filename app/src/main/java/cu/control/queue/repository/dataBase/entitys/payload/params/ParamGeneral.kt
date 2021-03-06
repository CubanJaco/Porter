package cu.control.queue.repository.dataBase.entitys.payload.params

import androidx.annotation.Keep
import cu.control.queue.repository.dataBase.entitys.payload.Person

@Keep
open class ParamGeneral(
    val person: Array<Person>? = null,
    val establishment: Int? = null,
    val info: Map<String, String>? = null,
    val created_date: Long? = null,
    val finished_date: Long? = null,
    val person_id: Array<String>? = null,
    val member_id: String? = null,
    val update_date: Long? = null
): Param()