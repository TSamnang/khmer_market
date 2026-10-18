package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.PackageStatus;
import com.example.khmer_market.domain.enums.ProductStatus;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.web.dto.PackageDto;
import com.example.khmer_market.web.dto.ProductFormDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPackageRepository productPackageRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final FileStorageService fileStorageService;

    @Value("${market.storage.max-product-images:5}")
    private int maxProductImages = 5;

    public ProductService(ProductRepository productRepository,
                          ProductPackageRepository productPackageRepository,
                          ProductImageRepository productImageRepository,
                          InventoryRepository inventoryRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          OrderItemRepository orderItemRepository,
                          FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.productPackageRepository = productPackageRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Product createProduct(Long sellerId, ProductFormDto dto) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));

        if (seller.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Pending or suspended farmers cannot publish products until approved by an administrator.");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId()));

        Product product = new Product();
        product.setSeller(seller);
        product.setCategory(category);
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        product.setStatus(ProductStatus.fromValue(dto.getStatus()));

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, Long sellerId, ProductFormDto dto) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId()));

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        product.setCategory(category);
        product.setStatus(ProductStatus.fromValue(dto.getStatus()));

        return productRepository.save(product);
    }

    @Transactional
    public String deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));

        long orderCount = orderItemRepository.countByProductId(productId);
        if (orderCount > 0) {
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            return "This product is referenced by existing customer orders and cannot be completely deleted. Its status has been set to 'inactive'.";
        }

        // Clean up stored image files
        for (ProductImage img : product.getImages()) {
            fileStorageService.deleteFile(img.getImageUrl());
        }

        productRepository.delete(product);
        return "Product deleted successfully.";
    }

    @Transactional
    public ProductImage addImage(Long productId, Long sellerId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));

        long currentImagesCount = productImageRepository.countByProductId(productId);
        if (currentImagesCount >= maxProductImages) {
            throw new IllegalArgumentException("Maximum of " + maxProductImages + " images allowed per product");
        }

        String filename = fileStorageService.storeFile(file);

        if (isPrimary || currentImagesCount == 0) {
            productImageRepository.clearPrimaryForProduct(productId);
            isPrimary = true;
        }

        ProductImage image = new ProductImage(product, filename, isPrimary);
        return productImageRepository.save(image);
    }

    @Transactional
    public void deleteImage(Long productId, Long sellerId, Long imageId) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));

        ProductImage image = productImageRepository.findById(imageId)
                .filter(img -> img.getProduct().getId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Image not found on this product"));

        boolean wasPrimary = image.isPrimary();
        fileStorageService.deleteFile(image.getImageUrl());
        productImageRepository.delete(image);

        if (wasPrimary) {
            List<ProductImage> remaining = productImageRepository.findByProductId(productId);
            if (!remaining.isEmpty()) {
                ProductImage nextPrimary = remaining.get(0);
                nextPrimary.setPrimary(true);
                productImageRepository.save(nextPrimary);
            }
        }
    }

    @Transactional
    public ProductPackage addPackage(Long productId, Long sellerId, PackageDto dto) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(product);
        pkg.setPackageName(dto.getPackageName().trim());
        pkg.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "kg");
        pkg.setUnitSize(dto.getUnitSize());
        pkg.setPrice(dto.getPrice() != null ? dto.getPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        pkg.setStatus(PackageStatus.fromValue(dto.getStatus()));

        String sku = (dto.getSku() != null && !dto.getSku().isBlank())
                ? dto.getSku().trim()
                : "SKU-" + productId + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        if (productPackageRepository.existsBySku(sku)) {
            sku = "SKU-" + productId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        pkg.setSku(sku);

        ProductPackage savedPackage = productPackageRepository.save(pkg);

        // Auto-create 1-1 inventory row
        Inventory inventory = new Inventory(savedPackage, dto.getQuantity() != null ? dto.getQuantity() : 0,
                dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : 5);
        inventoryRepository.save(inventory);

        return savedPackage;
    }

    @Transactional
    public ProductPackage updatePackage(Long packageId, Long sellerId, PackageDto dto) {
        ProductPackage pkg = productPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageId));

        if (!pkg.getProduct().getSeller().getId().equals(sellerId)) {
            throw new ResourceNotFoundException("Package not found or access denied");
        }

        pkg.setPackageName(dto.getPackageName().trim());
        pkg.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "kg");
        pkg.setUnitSize(dto.getUnitSize());
        pkg.setPrice(dto.getPrice() != null ? dto.getPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        pkg.setStatus(PackageStatus.fromValue(dto.getStatus()));

        if (dto.getSku() != null && !dto.getSku().isBlank() && !dto.getSku().equals(pkg.getSku())) {
            if (productPackageRepository.existsBySkuAndIdNot(dto.getSku().trim(), packageId)) {
                throw new IllegalArgumentException("SKU is already in use by another package");
            }
            pkg.setSku(dto.getSku().trim());
        }

        ProductPackage savedPackage = productPackageRepository.save(pkg);

        // Update inventory
        Inventory inv = inventoryRepository.findByProductPackageId(packageId)
                .orElseGet(() -> new Inventory(savedPackage, 0, 5));

        if (dto.getQuantity() != null) {
            if (dto.getQuantity() < inv.getReservedQuantity()) {
                throw new IllegalArgumentException("Physical stock quantity (" + dto.getQuantity() +
                        ") cannot be less than currently reserved quantity (" + inv.getReservedQuantity() + ")");
            }
            inv.setQuantity(dto.getQuantity());
        }
        if (dto.getLowStockThreshold() != null) {
            inv.setLowStockThreshold(dto.getLowStockThreshold());
        }
        inventoryRepository.save(inv);

        return savedPackage;
    }

    @Transactional
    public String deletePackage(Long packageId, Long sellerId) {
        ProductPackage pkg = productPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageId));

        if (!pkg.getProduct().getSeller().getId().equals(sellerId)) {
            throw new ResourceNotFoundException("Package not found or access denied");
        }

        long count = orderItemRepository.countByProductPackageId(packageId);
        if (count > 0) {
            pkg.setStatus(PackageStatus.INACTIVE);
            productPackageRepository.save(pkg);
            return "This package has historical orders and cannot be hard deleted. It has been marked 'inactive'.";
        }

        productPackageRepository.delete(pkg);
        return "Package removed successfully.";
    }

    @Transactional(readOnly = true)
    public Page<Product> getFarmerProducts(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable);
    }

    @Transactional(readOnly = true)
    public Product getProductForFarmer(Long productId, Long sellerId) {
        return productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied"));
    }

    @Transactional
    public Inventory updateInventory(Long packageId, Long sellerId, int totalQuantity, Integer lowStockThreshold) {
        ProductPackage pkg = productPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageId));

        if (!pkg.getProduct().getSeller().getId().equals(sellerId)) {
            throw new ResourceNotFoundException("Package not found or access denied");
        }

        Inventory inv = inventoryRepository.findByProductPackageId(packageId)
                .orElseGet(() -> new Inventory(pkg, 0, 5));

        if (totalQuantity < inv.getReservedQuantity()) {
            throw new IllegalArgumentException("Physical stock quantity (" + totalQuantity +
                    ") cannot be less than currently reserved quantity (" + inv.getReservedQuantity() + ")");
        }

        inv.setQuantity(totalQuantity);
        if (lowStockThreshold != null) {
            inv.setLowStockThreshold(lowStockThreshold);
        }
        return inventoryRepository.save(inv);
    }
}
