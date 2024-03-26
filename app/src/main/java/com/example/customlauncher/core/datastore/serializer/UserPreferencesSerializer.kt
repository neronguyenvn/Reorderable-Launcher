package com.example.customlauncher.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.customlauncher.core.common.coroutine.ClDispatcher.IO
import com.example.customlauncher.core.common.coroutine.Dispatcher
import com.example.customlauncher.core.datastore.model.UserPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class UserPreferencesSerializer @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher
) : Serializer<UserPreferences> {

    override val defaultValue: UserPreferences = UserPreferences()
    private val serializer = UserPreferences.serializer()

    override suspend fun readFrom(input: InputStream): UserPreferences =
        try {
            Json.decodeFromString(
                deserializer = serializer,
                string = input.readBytes().decodeToString()
            )
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read UserPreferences.", exception)
        }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) =
        withContext(ioDispatcher) {
            output.write(
                Json.encodeToString(serializer, t).encodeToByteArray()
            )
        }
}