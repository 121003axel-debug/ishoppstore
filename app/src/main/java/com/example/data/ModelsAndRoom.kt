package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Data model representing a Product in our catalog
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val rating: Double,
    val description: String,
    val imageUrl: String,
    val stock: Int = 10,
    val availableColors: List<String> = listOf("Azul", "Rojo", "Negro", "Plata"),
    val availableSizes: List<String> = listOf("S", "M", "L", "XL")
)

// 2. Room Entity for Shopping Cart Items
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val quantity: Int,
    val selectedColor: String,
    val selectedSize: String
)

// 3. Room Entity for Favorite Wishlist Products
@Entity(tableName = "favorite_items")
data class FavoriteEntity(
    @PrimaryKey val productId: String
)

// 4. Room Entity for Order History Log
@Entity(tableName = "orders_history")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val timestamp: Long,
    val totalAmount: Double,
    val itemsSummary: String, // Comma separated detail (e.g. "MacBook Air M3 x1, Sony Headphones x2")
    val status: String // e.g. "Procesado", "En Camino", "Entregado"
)

// 5. Room Data Access Object (DAO) for full state persistence
@Dao
interface ShopDao {
    // Cart operations
    @Query("SELECT * FROM cart_items")
    fun getCartItemsFlow(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun removeCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Favorite operations
    @Query("SELECT * FROM favorite_items")
    fun getFavoriteItemsFlow(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_items WHERE productId = :productId")
    suspend fun removeFavorite(productId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE productId = :productId)")
    suspend fun isFavorite(productId: String): Boolean

    // Order logs
    @Query("SELECT * FROM orders_history ORDER BY timestamp DESC")
    fun getOrdersHistoryFlow(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
}

// 6. DB Converters
class Converters {
    // Safe empty converter just in case Room needs advanced mappings, currently standard primitives
}

// 7. Room Database Creator
@Database(
    entities = [CartItemEntity::class, FavoriteEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ShopDatabase : RoomDatabase() {
    abstract val shopDao: ShopDao

    companion object {
        @Volatile
        private var INSTANCE: ShopDatabase? = null

        fun getDatabase(context: Context): ShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopDatabase::class.java,
                    "ishopp_store_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
