package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val shopDao = ShopDatabase.getDatabase(application).shopDao

    // Catalog of high-quality products
    val allProducts = listOf(
        Product(
            id = "1",
            name = "iPhone 15 Pro Max",
            category = "Electrónica",
            price = 1199.00,
            rating = 4.9,
            description = "Smartphone premium de última generación con procesador de alto rendimiento Apple A17 Pro (3nm), triple cámara principal de 48MP con lente periscópica, estructura completa de titanio cepillado, pantalla Super Retina XDR y puerto USB-C de ultra-velocidad. Calidad, estilo y velocidad inigualables.",
            imageUrl = "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=500",
            stock = 8,
            availableColors = listOf("Plata", "Negro", "Azul"),
            availableSizes = listOf("256GB", "512GB", "1TB")
        ),
        Product(
            id = "2",
            name = "MacBook Air M3",
            category = "Electrónica",
            price = 1099.00,
            rating = 4.8,
            description = "La laptop ultradelgada y veloz por excelencia. Equipada con el chip de última generación M3, pantalla Liquid Retina de 13.6 pulgadas con 500 nits de brillo, 16GB de memoria unificada y hasta 18 horas de autonomía excelente. Ideal para profesionales en movimiento.",
            imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500",
            stock = 5,
            availableColors = listOf("Medianoche", "Gris Espacial", "Plata"),
            availableSizes = listOf("256GB", "512GB")
        ),
        Product(
            id = "3",
            name = "Sony WH-1000XM5",
            category = "Electrónica",
            price = 399.00,
            rating = 4.7,
            description = "Audífonos inalámbricos circumaurales de diadema con la mejor cancelación activa de ruido (ANC) inteligente del mercado. Cuentan con el procesador integrado V1, sonido de alta resolución compatible con LDAC, sensor de proximidad e increíble autonomía de hasta 30 horas con carga rápida.",
            imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
            stock = 15,
            availableColors = listOf("Negro", "Plata Arena"),
            availableSizes = listOf("Único")
        ),
        Product(
            id = "4",
            name = "Chaqueta Deportiva Tech",
            category = "Moda",
            price = 79.99,
            rating = 4.5,
            description = "Chaqueta impermeable de gran estilo, transpirable y cortavientos, ideal para tus entrenamientos al aire libre o un estilo urbano refinado. Incluye bolsillos térmicos con cremalleras termoselladas y capucha ajustable oculta.",
            imageUrl = "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500",
            stock = 20,
            availableColors = listOf("Negro", "Verde Militar", "Gris"),
            availableSizes = listOf("S", "M", "L", "XL")
        ),
        Product(
            id = "5",
            name = "Tenis Deportivos iRun X",
            category = "Moda",
            price = 120.00,
            rating = 4.6,
            description = "Tenis ultra-ligeros para corredores de alto rendimiento. Confeccionados con malla transpirable, suela intermedia con retorno de energía dinámico y forro para absorción de impactos. Corrre tus mejores carreras cómodamente sin esfuerzo.",
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500",
            stock = 12,
            availableColors = listOf("Rojo", "Azul", "Blanco"),
            availableSizes = listOf("38", "39", "40", "41", "42", "43")
        ),
        Product(
            id = "6",
            name = "SmartWatch ActiveFit",
            category = "Moda",
            price = 199.99,
            rating = 4.4,
            description = "Lleva el monitoreo detallado de tu salud al siguiente nivel. Equipado con GPS integrado, medidor avanzado de composición corporal, ritmo cardíaco continuo, oxígeno en sangre (SpO2) y seguimiento avanzado del sueño. Resistente al agua hasta 50 metros.",
            imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500",
            stock = 10,
            availableColors = listOf("Negro Brillante", "Rosa Mate", "Plata"),
            availableSizes = listOf("40mm", "44mm")
        ),
        Product(
            id = "7",
            name = "Lámpara de Escritorio Nórdica",
            category = "Hogar",
            price = 45.50,
            rating = 4.5,
            description = "Lámpara de mesa minimalista con diseño escandinavo en madera noble y metal esmaltado blanco. Cuenta con un práctico cargador inalámbrico rápido Qi de 10W integrado en la base de madera y sensor táctil con tres tonos de luz cálida y neutra.",
            imageUrl = "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500",
            stock = 18,
            availableColors = listOf("Blanco Madera"),
            availableSizes = listOf("Estándar")
        ),
        Product(
            id = "8",
            name = "Mochila Ergonómica Ejecutiva",
            category = "Hogar",
            price = 85.00,
            rating = 4.6,
            description = "Mochila premium antirrobo con puerto USB de carga rápida exterior para banco de energía. Cuenta con compartimentos acolchados dedicados para laptop de 16 pulgadas y tablet, interior repelente al agua, y espaldar diseñado ergonómicamente de gran frescura.",
            imageUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500",
            stock = 14,
            availableColors = listOf("Gris Carbón", "Negro"),
            availableSizes = listOf("Único")
        )
    )

    // Search and filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Todos")

    // UI lists filtering dynamically
    val filteredProducts: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory
    ) { query, category ->
        allProducts.filter { product ->
            val matchesSearch = product.name.contains(query, ignoreCase = true) || 
                                product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "Todos" || product.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allProducts)

    // Categories list based on catalog
    val categories = listOf("Todos", "Electrónica", "Moda", "Hogar")

    // Favorites integration
    val favoriteIdsSet: StateFlow<Set<String>> = shopDao.getFavoriteItemsFlow()
        .map { list -> list.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Cart mapping helper class
    data class CartItem(
        val product: Product,
        val quantity: Int,
        val selectedColor: String,
        val selectedSize: String
    ) {
        val totalCost: Double get() = product.price * quantity
    }

    // Mapping Cart Items entities to detailed CartItems containing Product items
    val cartItemsList: StateFlow<List<CartItem>> = shopDao.getCartItemsFlow()
        .map { entities ->
            entities.mapNotNull { entity ->
                val prod = allProducts.find { it.id == entity.productId }
                if (prod != null) {
                    CartItem(
                        product = prod,
                        quantity = entity.quantity,
                        selectedColor = entity.selectedColor,
                        selectedSize = entity.selectedSize
                    )
                } else null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated fields
    val cartTotalPrice: StateFlow<Double> = cartItemsList.map { cartList ->
        cartList.sumOf { it.totalCost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = cartItemsList.map { cartList ->
        cartList.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Order logs flow
    val ordersHistory: StateFlow<List<OrderEntity>> = shopDao.getOrdersHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Checkout flags
    val isCheckingOut = MutableStateFlow(false)
    val checkoutComplete = MutableStateFlow(false)

    // Selected product detail modal
    val selectedProduct = MutableStateFlow<Product?>(null)

    // AI Sales Assitant chat flow
    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            Pair("¡Hola! Soy tu asistente inteligente de iShopp. Puedo ayudarte a encontrar el producto ideal, darte consejos, explicarte características o guiarte en tu proceso de compra. ¿Qué estás buscando hoy?", false)
        )
    )
    val aiMessages: StateFlow<List<Pair<String, Boolean>>> = _aiMessages.asStateFlow()
    val isAiLoading = MutableStateFlow(false)

    // Cart actions
    fun addToCart(product: Product, color: String, size: String, quantity: Int) {
        viewModelScope.launch {
            val entity = CartItemEntity(
                productId = product.id,
                quantity = quantity,
                selectedColor = color,
                selectedSize = size
            )
            shopDao.insertCartItem(entity)
        }
    }

    fun updateCartQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            if (quantity <= 0) {
                removeFromCart(productId)
            } else {
                val existing = cartItemsList.value.find { it.product.id == productId }
                if (existing != null) {
                    shopDao.insertCartItem(
                        CartItemEntity(
                            productId = productId,
                            quantity = quantity,
                            selectedColor = existing.selectedColor,
                            selectedSize = existing.selectedSize
                        )
                    )
                }
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            shopDao.removeCartItem(productId)
        }
    }

    // Wishlist actions
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            if (favoriteIdsSet.value.contains(productId)) {
                shopDao.removeFavorite(productId)
            } else {
                shopDao.addFavorite(FavoriteEntity(productId))
            }
        }
    }

    // checkout action
    fun checkoutCart() {
        viewModelScope.launch {
            val total = cartTotalPrice.value
            val cartList = cartItemsList.value
            if (cartList.isEmpty()) return@launch

            isCheckingOut.value = true
            // Simulate processing time
            kotlinx.coroutines.delay(1800)

            val summary = cartList.joinToString(", ") { "${it.product.name} (x${it.quantity})" }
            val order = OrderEntity(
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                itemsSummary = summary,
                status = "Procesado"
            )
            shopDao.insertOrder(order)
            shopDao.clearCart()

            isCheckingOut.value = false
            checkoutComplete.value = true
        }
    }

    fun resetCheckout() {
        checkoutComplete.value = false
    }

    // Gemini dynamic AI assistant interaction
    fun askAssistant(userMessage: String) {
        if (userMessage.isBlank()) return
        // Add user message locally
        _aiMessages.update { list -> list + Pair(userMessage, true) }
        isAiLoading.value = true

        viewModelScope.launch {
            // Build Context context grounded for Gemini model
            val catalogDescription = allProducts.joinToString("\n") { p ->
                "- ID ${p.id}: ${p.name} en ${p.category} ($${p.price}) - ${p.description}"
            }

            val systemPrompt = """
                Eres un Asistente Comercial de E-Commerce experto y carismático para la tienda de tecnología y estilo de vida "iShopp Store".
                Tu objetivo es orientar y deleitar a los clientes, recomendándoles productos basados en el catálogo que se encuentra a continuación.
                Sé breve, cálido, cortés, persuasivo y responde en español.
                Si el usuario pregunta por ofertas, recomiéndale marcas del catálogo y destaca que el iPhone 15 Pro Max, los Sony WH-1000XM5 y la MacBook Air M3 son nuestros artículos estrella.
                Sé honesto: si te preguntan sobre un producto que no está en el catálogo, puedes mencionarlo brevemente pero redirige alegremente la atención hacia algo similar o complementario de nuestro inventario de iShopp.
                
                CATÁLOGO DE ISHOPP STORE:
                $catalogDescription
            """.trimIndent()

            val aiAnswer = GeminiApiClient.askGemini(prompt = userMessage, systemInstruction = systemPrompt)
            isAiLoading.value = false
            _aiMessages.update { list -> list + Pair(aiAnswer, false) }
        }
    }
}

class ShopViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            return ShopViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
