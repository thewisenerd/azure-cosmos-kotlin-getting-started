import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.sample.common.AccountSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.system.exitProcess

fun main2(container: CosmosAsyncContainer) = runBlocking {
    val value = UUID.randomUUID().toString()

    val item =  {
        mapOf(
            "id" to UUID.randomUUID().toString(),
            "value" to value
        )
    }

    val all = (1..200).map { idx ->
        GlobalScope.async {
            println("async-ing $idx")
            container.createItem(item()).toFuture().await()
        }
    }.awaitAll()

    all.map { it.item }.forEachIndexed { index, map ->
        println("$index => $map")
    }
}

fun main() = runBlocking<Unit> {
    val DATABASE = "test"
    val COLLECTION = "cosmos_db_dao_${System.currentTimeMillis()}"

    val cosmosClient = CosmosClientBuilder()
        .endpoint(AccountSettings.HOST)
        .key(AccountSettings.MASTER_KEY)
        .consistencyLevel(ConsistencyLevel.SESSION)
        .contentResponseOnWriteEnabled(true)
        .directMode()
        .buildAsyncClient()
    cosmosClient.createDatabaseIfNotExists(DATABASE).toFuture().await()
    val database = cosmosClient.getDatabase(DATABASE)
    database.createContainerIfNotExists(COLLECTION, "/id").toFuture().await()
    val container = database.getContainer(COLLECTION)

    try {
        main2(container)
    } finally {
        container.delete()
    }

    exitProcess(0)
}