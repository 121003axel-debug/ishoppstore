package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.OrderEntity
import com.example.data.Product
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.ShopViewModel.CartItem
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopAppUI(viewModel: ShopViewModel) {
    var currentTab by remember { mutableStateOf(0) }
    
    val cartCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val checkoutComplete by viewModel.checkoutComplete.collectAsStateWithLifecycle()
    var showPaymentDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "iShopp logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "iShopp Store",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentTab = 2 },
                        modifier = Modifier.testTag("action_tab_cart")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text(text = cartCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Ver Carrito"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") },
                    modifier = Modifier.testTag("tab_favorites")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text(cartCount.toString()) }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    },
                    label = { Text("Carrito") },
                    modifier = Modifier.testTag("tab_cart")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = "Asistente AI") },
                    label = { Text("Asistente AI") },
                    modifier = Modifier.testTag("tab_ai")
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Pedidos") },
                    modifier = Modifier.testTag("tab_orders")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> StorefrontScreen(viewModel)
                1 -> FavoritesScreen(viewModel)
                2 -> CartScreen(viewModel, onPaymentRequested = { showPaymentDialog = true })
                3 -> AiConciergeScreen(viewModel)
                4 -> OrdersScreen(viewModel)
            }

            // Floating helper to activate Gemini AI Assistant instantly
            if (currentTab != 3) {
                FloatingActionButton(
                    onClick = { currentTab = 3 },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("fab_ai_shortcut"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat de Compras"
                    )
                }
            }
        }
    }

    // Interactive details Modal sheet overlay
    selectedProduct?.let { product ->
        ProductDetailsDialog(
            product = product,
            onDismiss = { viewModel.selectedProduct.value = null },
            onAddToCart = { color, size, qty ->
                viewModel.addToCart(product, color, size, qty)
                viewModel.selectedProduct.value = null
                // Show Snackbar
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡${product.name} agregado al carrito!")
                }
            },
            isFavorite = viewModel.favoriteIdsSet.value.contains(product.id),
            onToggleFavorite = { viewModel.toggleFavorite(product.id) }
        )
    }

    // Checkout Virtual Card Payment Dialog
    if (showPaymentDialog) {
        PaymentCheckoutDialog(
            viewModel = viewModel,
            onDismiss = { showPaymentDialog = false }
        )
    }

    // Purchase Order Celebration Dialog
    if (checkoutComplete) {
        OrderSuccessDialog(
            onDismiss = {
                viewModel.resetCheckout()
                currentTab = 4 // Redirect to history receipts
            }
        )
    }
}

// ----------------------------------------
// SCREEN 1: STOREFRONT (INICIO)
// ----------------------------------------
@Composable
fun StorefrontScreen(viewModel: ShopViewModel) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIdsSet.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCat by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            maxLines = 1,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            placeholder = { Text("¿Qué deseas comprar hoy?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_bar"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // 2. Campaign banner billboard
            item {
                BillboardPromoBanner()
            }

            // 3. Horizontal Categories Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.categories.forEach { category ->
                        val isSelected = selectedCat.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectedCategory.value = category
                            },
                            label = { Text(text = category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 4. Compact Product Catalog Grid list (implemented inside standard list for safety)
            if (products.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Sin productos",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No encontramos lo que buscas",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Intenta buscar de otra manera o explora las categorías.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Productos Destacados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                // Chunk elements into pairs of 2 to display them as a neat grid inside LazyColumn
                val pairs = products.chunked(2)
                items(pairs) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (prod in pair) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_card_${prod.id}")
                            ) {
                                ProductItemCard(
                                    product = prod,
                                    isFavorite = favorites.contains(prod.id),
                                    onFavoriteClick = { viewModel.toggleFavorite(prod.id) },
                                    onProductSelect = { viewModel.selectedProduct.value = prod }
                                )
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillboardPromoBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                SuggestionChip(
                    onClick = {},
                    label = { Text("OFERTA ESPECIAL", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White.copy(alpha = 0.3f),
                        labelColor = Color.White
                    ),
                    border = null,
                    modifier = Modifier.height(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Envíos Gratis en iShopp",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Aprovecha compras seguras con 12 MSI pagando con tarjeta.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onProductSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.LightGray.copy(alpha = 0.15f))
            ) {
                // Async image from Unsplash
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = null // fallbacks to drawing
                )

                // High-contrast fallback vector drawing when image can't load
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.id == "0") { // If we ever fall back or have explicit drawing
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "iShopp Cart logo",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Category Tag/Badge
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.category,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Heart wishlist button float
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color.Red else Color.DarkGray,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Description body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Calificación",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = product.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "$${product.price}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Agregar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onProductSelect() }
                    )
                }
            }
        }
    }
}

// ----------------------------------------
// SCREEN 2: FAVORITES (FAVORITOS)
// ----------------------------------------
@Composable
fun FavoritesScreen(viewModel: ShopViewModel) {
    val favoriteIds by viewModel.favoriteIdsSet.collectAsStateWithLifecycle()
    val favoritesList = remember(favoriteIds) {
        viewModel.allProducts.filter { favoriteIds.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Favoritos",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favoritesList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favoritos vacío",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu lista de deseos está vacía",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Toca el corazón de los productos para guardarlos aquí.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favoritesList) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectedProduct.value = product },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(90.dp)
                                    .fillMaxHeight()
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = product.category,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "$${product.price}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleFavorite(product.id) },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Quitar",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// SCREEN 3: SHOPPING CART (CARRITO)
// ----------------------------------------
@Composable
fun CartScreen(viewModel: ShopViewModel, onPaymentRequested: () -> Unit) {
    val items by viewModel.cartItemsList.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartTotalPrice.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mi Carrito",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Carrito vacío",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu carrito está vacío",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Explora nuestro catálogo y agrega excelentes productos.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).padding(top = 4.dp)
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Cart Products Scroll Lists
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        CartItemRow(item, viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt price breakdown card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Subtotal", fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", subtotal)}", fontSize = 13.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val tax = subtotal * 0.16
                            Text("IVA (16% local)", fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", tax)}", fontSize = 13.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Envío estimado", fontSize = 13.sp)
                            Text("Gratis", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val total = subtotal * 1.16
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("$${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPaymentRequested,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("checkout_pay_button"),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Pago Seguro")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proceder al Pago Seguro", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: ShopViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = item.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Vara: ${item.selectedColor}, ${item.selectedSize}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.totalCost)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = item.quantity.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Mas", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            IconButton(
                onClick = { viewModel.removeFromCart(item.product.id) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Quitar del carrito",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ----------------------------------------
// SCREEN 4: GEMINI AI CONCIERGE CHAT
// ----------------------------------------
@Composable
fun AiConciergeScreen(viewModel: ShopViewModel) {
    val messages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var chatText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll bottom when new messages come
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Asistente Personal iShopp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Asesor de compras de la tienda impulsado por Gemini AI", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Chat logs
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.second
                val text = msg.first

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Top)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("IA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Top)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Tú", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Pulsing loader while Gemini solves background
            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("iShopp está pensando...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // Recommended chips row to quickly interact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val prompts = listOf(
                "¿Qué laptops tienen disponibles?",
                "¿Tienen ofertas en celulares?",
                "¿Hay envíos gratis?",
                "Recomiéndame audífonos"
            )
            prompts.forEach { text ->
                InputChip(
                    selected = false,
                    onClick = { viewModel.askAssistant(text) },
                    label = { Text(text = text, fontSize = 11.sp) }
                )
            }
        }

        // Chat Input panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Preguntar al Asistente e-commerce...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (chatText.isNotBlank()) {
                            viewModel.askAssistant(chatText)
                            chatText = ""
                            keyboardController?.hide()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (chatText.isNotBlank()) {
                        viewModel.askAssistant(chatText)
                        chatText = ""
                        keyboardController?.hide()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ----------------------------------------
// SCREEN 5: PLACED ORDER HISTORIAL
// ----------------------------------------
@Composable
fun OrdersScreen(viewModel: ShopViewModel) {
    val orders by viewModel.ordersHistory.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Pedidos",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (orders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Sin pedidos",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aún no has realizado pedidos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tus compras seguras e historiales se registrarán en esta sección.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(orders) { order ->
                    OrderHistoryItem(order)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryItem(order: OrderEntity) {
    val dateStr = remember(order.timestamp) {
        val sdf = SimpleDateFormat("dd 'de' MMM, yyyy - hh:mm a", Locale("es", "ES"))
        sdf.format(Date(order.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido #SHP-${10000 + order.orderId}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Exitoso",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Artículos:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = order.itemsSummary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Timeline Delivery tracking bar design
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Estado: En preparación logística",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Simulated linear progress indicator
                LinearProgressIndicator(
                    progress = { 0.35f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Pagado:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.totalAmount * 1.16)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// ----------------------------------------
// OVERLAY MODAL: PRODUCT DETAILS DIALOG
// ----------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsDialog(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (color: String, size: String, quantity: Int) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(product.availableColors.first()) }
    var selectedSize by remember { mutableStateOf(product.availableSizes.first()) }
    var buyQuantity by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", modifier = Modifier.padding(6.dp))
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Me Gusta",
                                tint = if (isFavorite) Color.Red else Color.DarkGray,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                // Core content
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${product.price}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Rating Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(product.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•  Unidad: ${product.stock} restantes", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = product.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Variant 1: Color pickers (Circular bubbles)
                    Text("Selecciona Color:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product.availableColors.forEach { colorName ->
                            val isSelected = selectedColor == colorName
                            val mappedColor = when (colorName) {
                                "Azul" -> Color(0xFF1976D2)
                                "Rojo" -> Color(0xFFD32F2F)
                                "Negro" -> Color(0xFF212121)
                                "Plata" -> Color(0xFFB0BEC5)
                                "Medianoche" -> Color(0xFF1A237E)
                                "Gris Espacial" -> Color(0xFF37474F)
                                "Gris" -> Color(0xFF78909C)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(mappedColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorName },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Variant 2: Size pickers (Pill bubbles)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Selecciona Capacidad / Medida:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        product.availableSizes.forEach { sizeName ->
                            val isSelected = selectedSize == sizeName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSize = sizeName },
                                label = { Text(sizeName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Quantity spinner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cantidad:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { if (buyQuantity > 1) buyQuantity-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos")
                            }
                            Text(
                                text = buyQuantity.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (buyQuantity < product.stock) buyQuantity++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Add to cart main buttons
                    Button(
                        onClick = { onAddToCart(selectedColor, selectedSize, buyQuantity) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("add_to_cart_detailed"),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agregar al Carrito - $${String.format(Locale.US, "%.2f", product.price * buyQuantity)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// OVERLAY MODAL: SECURE CREDIT CARD PAYMENT SCREEN
// ----------------------------------------
@Composable
fun PaymentCheckoutDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val isCheckingOut by viewModel.isCheckingOut.collectAsStateWithLifecycle()
    val totalAmount by viewModel.cartTotalPrice.collectAsStateWithLifecycle()

    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    var localError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = { if (!isCheckingOut) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillModifierWithSoftKeyboard()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pago Seguro iShopp",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, enabled = !isCheckingOut) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Virtual iShopp Credit Card graphic representation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF002244),
                                        Color(0xFF2C5E8A),
                                        Color(0xFFD32F2F)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "VISA iShopp Card",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = if (cardNumber.isBlank()) "•••• •••• •••• ••••" else cardNumber.chunked(4).joinToString(" "),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                                letterSpacing = 2.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("TITULAR", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text(
                                        text = if (cardHolder.isBlank()) "NOMBRE CLIENTE" else cardHolder.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EXPIRA", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text(
                                        text = if (cardExpiry.isBlank()) "MM/AA" else cardExpiry,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card forms inputs
                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Nombre del Titular") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_holder"),
                    singleLine = true,
                    enabled = !isCheckingOut,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16 && it.all { ch -> ch.isDigit() }) cardNumber = it },
                    label = { Text("Número de Tarjeta") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_number"),
                    singleLine = true,
                    enabled = !isCheckingOut,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { if (it.length <= 5) cardExpiry = it },
                        label = { Text("Exp (MM/AA)") },
                        modifier = Modifier.weight(1f).testTag("payment_expiry"),
                        singleLine = true,
                        enabled = !isCheckingOut,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )

                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) cardCvv = it },
                        label = { Text("CVV") },
                        modifier = Modifier.weight(1f).testTag("payment_cvv"),
                        singleLine = true,
                        enabled = !isCheckingOut,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                if (localError.isNotBlank()) {
                    Text(
                        text = localError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive touch safe trigger check button with loaders
                Button(
                    onClick = {
                        if (cardHolder.isBlank() || cardNumber.length < 13 || cardExpiry.length < 5 || cardCvv.length < 3) {
                            localError = "Por favor ingresa datos de tarjeta válidos."
                        } else {
                            localError = ""
                            focusManager.clearFocus()
                            viewModel.checkoutCart()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pay_confirm_checkout"),
                    enabled = !isCheckingOut,
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isCheckingOut) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Procesando iShopp Secure...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pagar Ahora: $${String.format(Locale.US, "%.2f", totalAmount * 1.16)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// OVERLAY MODAL: CELEBRATION ORDER SUCCESS
// ----------------------------------------
@Composable
fun OrderSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success anim icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Compra Exitosa en iShopp!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Tu orden ha sido procesada mediante pago seguro. Puedes revisar su estado logístico en tiempo real.",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                    lineHeight = 16.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_success_dialog"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Seguir Mis Productos", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Custom soft keyboard and window state resize support helper
fun Modifier.fillModifierWithSoftKeyboard(): Modifier {
    return this.fillMaxWidth()
}
