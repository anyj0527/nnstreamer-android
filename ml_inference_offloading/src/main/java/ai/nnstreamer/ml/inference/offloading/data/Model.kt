package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * The entity class for the models table.
 *
 * @constructor Creates a Model object.
 * @property framework The neural network framework supporting this model.
 * @property models The file names of this model.
 * @property inputInfo A map that represents the input information of the model.
 * @property outputInfo A map that represents the output information of the model.
 * @property optionalInfo A map that extensible information of the model.
 * @pram uid The unique ID of the model.
 * @pram name The name that represents this model.
 * @pram jsonObject The JSON object corresponding to 'single'.
 * @pram optionalJsonObject The JSON object corresponding to 'information'.
 */
@Entity(tableName = "models")
data class Model(
    @PrimaryKey
    val uid: Int = 0,
    val name: String,
) {
    constructor(
        uid: Int,
        name: String,
        jsonObject: JSONObject = JSONObject(),
        optionalJsonObject: JSONObject = JSONObject()
    ) : this(uid, name) {
        // TODO: The following is the parser for "single", which is a legacy conf
        runCatching {
            framework = jsonObject.getString("framework")
        }.exceptionOrNull()

        runCatching {
            val modelArray = jsonObject.getJSONArray("model")
            val modelPaths = mutableListOf<String>()

            for (i in 0 until modelArray.length()) {
                val model = modelArray.getString(i)

                modelPaths.add(File(model).name)
            }
            models = modelPaths.joinToString(",")
        }.onFailure { e ->
            when (e) {
                is JSONException -> {
                    val model = jsonObject.getString("model")
                    models = File(model).name
                }

                else -> {
                    throw e
                }
            }
        }.exceptionOrNull()

        listOf("input_info", "output_info").forEach { prop ->
            runCatching {
                val infoMap = mutableMapOf<String, MutableList<String>>(
                    "type" to mutableListOf(),
                    "dimension" to mutableListOf()
                )

                val info = jsonObject.getJSONArray(prop)

                for (i in 0 until info.length()) {
                    val obj = info.getJSONObject(i)

                    infoMap.keys.forEach { key ->
                        if (obj.has(key)) {
                            infoMap[key]?.add(obj.getString(key))
                        }
                    }
                }

                when (prop) {
                    "input_info" -> inputInfo = infoMap.toMap()
                    "output_info" -> outputInfo = infoMap.toMap()
                }
            }.exceptionOrNull()
        }

        val optionalInformationMap = mutableMapOf<String, String>()

        optionalJsonObject.keys().forEach { key ->
            optionalInformationMap[key] = ""
        }
        runCatching {
            optionalInformationMap.keys.forEach { key ->
                optionalInformationMap[key] = optionalJsonObject.getString(key)
            }
        }.exceptionOrNull()

        optionalInfo = optionalInformationMap
    }

    var framework: String = ""
    var models: String = ""

    @ColumnInfo(name = "input_info")
    var inputInfo: Map<String, List<String>> = mapOf()

    @ColumnInfo(name = "output_info")
    var outputInfo: Map<String, List<String>> = mapOf()

    var optionalInfo: Map<String, String> = mapOf()
}
