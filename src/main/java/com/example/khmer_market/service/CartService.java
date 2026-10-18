package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.PackageStatus;
import com.example.khmer_market.exception.OutOfStockException;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.web.dto.CartItemViewDto;
import com.example.khmer_market.web.dto.CartViewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    public static final int MAX_CART_LINES = 50;
    public static final int MAX_QUANTITY_PER_LINE = 100;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductPackageRepository productPackageRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private StoreConfigService storeConfigService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setStoreConfigService(StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductPackageRepository productPackageRepository,
                       InventoryRepository inventoryRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productPackageRepository = productPackageRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            Cart cart = new Cart(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public void addToCart(Long userId, Long packageId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = getOrCreateCart(userId);

        ProductPackage pkg = productPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product package not found: " + packageId));

        if (!pkg.isActive() || !pkg.getProduct().isActive() || !pkg.getProduct().getSeller().isActive()) {
            throw new IllegalArgumentException("This product or package is no longer available");
        }

        if (pkg.getProduct().getSeller() != null && pkg.getProduct().getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot purchase your own product");
        }

        Inventory inv = inventoryRepository.findByProductPackageId(packageId).orElse(null);
        int available = (inv != null) ? inv.getAvailableQuantity() : 0;
        if (available <= 0) {
            throw new OutOfStockException("This package is currently out of stock");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductPackageId(cart.getId(), packageId);

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            int targetQty = existing.getQuantity() + quantity;
            if (targetQty > MAX_QUANTITY_PER_LINE) {
                throw new IllegalArgumentException("Maximum quantity of " + MAX_QUANTITY_PER_LINE + " per line exceeded");
            }
            if (targetQty > available) {
                throw new OutOfStockException("Only " + available + " units are available in stock");
            }
            existing.setQuantity(targetQty);
            existing.setUnitPrice(pkg.getPrice());
            cartItemRepository.save(existing);
        } else {
            long currentLines = cartItemRepository.countByCartId(cart.getId());
            if (currentLines >= MAX_CART_LINES) {
                throw new IllegalArgumentException("Cart cannot contain more than " + MAX_CART_LINES + " unique items");
            }
            if (quantity > MAX_QUANTITY_PER_LINE) {
                throw new IllegalArgumentException("Maximum quantity of " + MAX_QUANTITY_PER_LINE + " per line exceeded");
            }
            if (quantity > available) {
                throw new OutOfStockException("Only " + available + " units are available in stock");
            }

            CartItem newItem = new CartItem(cart, pkg, quantity, pkg.getPrice());
            cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public CartViewDto getCartView(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        CartViewDto view = new CartViewDto();
        view.setId(cart.getId());

        BigDecimal total = BigDecimal.ZERO;
        int totalItems = 0;
        boolean priceChangedOverall = false;
        boolean hasInactiveOrOut = false;

        for (CartItem item : items) {
            ProductPackage pkg = item.getProductPackage();
            Product product = pkg.getProduct();

            CartItemViewDto itemDto = new CartItemViewDto();
            itemDto.setId(item.getId());
            itemDto.setPackageId(pkg.getId());
            itemDto.setPackageName(pkg.getPackageName());
            itemDto.setProductId(product.getId());
            itemDto.setProductName(product.getName());
            itemDto.setSellerId(product.getSeller().getId());

            FarmerProfile farm = product.getSeller().getFarmerProfile();
            itemDto.setFarmName(farm != null ? farm.getFarmName() : product.getSeller().getUsername());

            ProductImage primaryImg = product.getPrimaryImage();
            itemDto.setImageUrl(primaryImg != null ? primaryImg.getImageUrl() : null);

            // Re-sync price to current package price
            BigDecimal currentPrice = pkg.getPrice();
            BigDecimal storedPrice = item.getUnitPrice();
            itemDto.setUnitPrice(currentPrice);
            itemDto.setOldPrice(storedPrice);

            boolean itemPriceChanged = storedPrice.compareTo(currentPrice) != 0;
            if (itemPriceChanged) {
                priceChangedOverall = true;
                itemDto.setPriceChanged(true);
                // Auto-sync stored unit price
                item.setUnitPrice(currentPrice);
                cartItemRepository.save(item);
            }

            Inventory inv = inventoryRepository.findByProductPackageId(pkg.getId()).orElse(null);
            int available = (inv != null) ? inv.getAvailableQuantity() : 0;
            itemDto.setAvailableStock(available);

            boolean isInactive = (pkg.getStatus() != PackageStatus.ACTIVE) || !product.isActive() || !product.getSeller().isActive();
            boolean isOutOfStock = available <= 0 || item.getQuantity() > available;

            itemDto.setInactive(isInactive);
            itemDto.setOutOfStock(isOutOfStock);

            if (isInactive || isOutOfStock) {
                hasInactiveOrOut = true;
            }

            itemDto.setQuantity(item.getQuantity());
            BigDecimal lineSubtotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            itemDto.setSubtotal(lineSubtotal);

            if (!isInactive && !isOutOfStock) {
                total = total.add(lineSubtotal);
            }
            totalItems += item.getQuantity();

            view.getItems().add(itemDto);
        }

        // Compute multi-vendor shipping per distinct active farmer
        BigDecimal effectiveThreshold = (storeConfigService != null) ? storeConfigService.getShippingFreeThreshold() : new BigDecimal("25.00");
        BigDecimal effectiveFlatRate = (storeConfigService != null) ? storeConfigService.getShippingFlatRate() : new BigDecimal("1.50");

        java.util.Map<Long, BigDecimal> sellerTotals = new java.util.HashMap<>();
        for (CartItemViewDto itemDto : view.getItems()) {
            if (!itemDto.isInactive() && !itemDto.isOutOfStock()) {
                sellerTotals.put(itemDto.getSellerId(), sellerTotals.getOrDefault(itemDto.getSellerId(), BigDecimal.ZERO).add(itemDto.getSubtotal()));
            }
        }

        BigDecimal totalShipping = BigDecimal.ZERO;
        for (BigDecimal sTotal : sellerTotals.values()) {
            if (sTotal.compareTo(effectiveThreshold) < 0) {
                totalShipping = totalShipping.add(effectiveFlatRate);
            }
        }

        view.setSubtotal(total.setScale(2, RoundingMode.HALF_UP));
        view.setShippingFee(totalShipping.setScale(2, RoundingMode.HALF_UP));
        view.setGrandTotal(total.add(totalShipping).setScale(2, RoundingMode.HALF_UP));
        view.setTotalItemCount(totalItems);
        view.setPriceChanged(priceChangedOverall);
        view.setHasInactiveOrOutOfStockItems(hasInactiveOrOut);

        return view;
    }

    @Transactional
    public void updateItemQuantity(Long userId, Long cartItemId, int newQuantity) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found or access denied"));

        if (newQuantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }

        if (newQuantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException("Maximum quantity of " + MAX_QUANTITY_PER_LINE + " per line exceeded");
        }

        Inventory inv = inventoryRepository.findByProductPackageId(item.getProductPackage().getId()).orElse(null);
        int available = (inv != null) ? inv.getAvailableQuantity() : 0;
        if (newQuantity > available) {
            throw new OutOfStockException("Cannot set quantity to " + newQuantity + ". Only " + available + " units available.");
        }

        item.setQuantity(newQuantity);
        item.setUnitPrice(item.getProductPackage().getPrice());
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found or access denied"));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteAllByCartId(cart.getId()));
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = cartItemRepository.sumQuantityByUserId(userId);
        return count != null ? count.intValue() : 0;
    }
}
