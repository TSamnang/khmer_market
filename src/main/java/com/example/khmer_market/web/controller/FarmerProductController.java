package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Category;
import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.entity.ProductPackage;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CategoryService;
import com.example.khmer_market.service.ProductService;
import com.example.khmer_market.web.dto.PackageDto;
import com.example.khmer_market.web.dto.ProductFormDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/farmer/products")
public class FarmerProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public FarmerProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listProducts(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        Page<Product> products = productService.getFarmerProducts(userDetails.getUserId(), PageRequest.of(Math.max(0, page), size));
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        return "farmer/products";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        List<Category> categories = categoryService.findAllActive();
        model.addAttribute("productForm", new ProductFormDto());
        model.addAttribute("categories", categories);
        return "farmer/product-form";
    }

    @PostMapping("/new")
    public String createProduct(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute("productForm") ProductFormDto productForm,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                @RequestParam(value = "packageName", defaultValue = "Standard Pack") String packageName,
                                @RequestParam(value = "packagePrice", defaultValue = "10.00") BigDecimal packagePrice,
                                @RequestParam(value = "packageUnit", defaultValue = "kg") String packageUnit,
                                @RequestParam(value = "initialStock", defaultValue = "50") Integer initialStock,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAllActive());
            return "farmer/product-form";
        }

        try {
            Product product = productService.createProduct(userDetails.getUserId(), productForm);

            // Add initial package
            PackageDto pkgDto = new PackageDto();
            pkgDto.setPackageName(packageName);
            pkgDto.setPrice(packagePrice);
            pkgDto.setUnit(packageUnit);
            pkgDto.setQuantity(initialStock);
            pkgDto.setLowStockThreshold(5);
            productService.addPackage(product.getId(), userDetails.getUserId(), pkgDto);

            // Upload initial images if provided
            if (imageFiles != null) {
                boolean first = true;
                for (MultipartFile file : imageFiles) {
                    if (file != null && !file.isEmpty()) {
                        productService.addImage(product.getId(), userDetails.getUserId(), file, first);
                        first = false;
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully with package options!");
            return "redirect:/farmer/products/" + product.getId() + "/edit";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.findAllActive());
            return "farmer/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        Product product = productService.getProductForFarmer(id, userDetails.getUserId());
        ProductFormDto form = new ProductFormDto();
        form.setId(product.getId());
        form.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setStatus(product.getStatus().getValue());

        model.addAttribute("product", product);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", categoryService.findAllActive());
        model.addAttribute("packageDto", new PackageDto());

        return "farmer/product-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute("productForm") ProductFormDto productForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Product product = productService.getProductForFarmer(id, userDetails.getUserId());
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("packageDto", new PackageDto());
            return "farmer/product-edit";
        }

        try {
            productService.updateProduct(id, userDetails.getUserId(), productForm);
            redirectAttributes.addFlashAttribute("successMessage", "Product details updated successfully");
            return "redirect:/farmer/products/" + id + "/edit";
        } catch (Exception e) {
            Product product = productService.getProductForFarmer(id, userDetails.getUserId());
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("packageDto", new PackageDto());
            model.addAttribute("errorMessage", e.getMessage());
            return "farmer/product-edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            String msg = productService.deleteProduct(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/products";
    }

    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam("imageFile") MultipartFile file,
                              @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
                              RedirectAttributes redirectAttributes) {
        try {
            if (file != null && !file.isEmpty()) {
                productService.addImage(id, userDetails.getUserId(), file, isPrimary);
                redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/products/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id,
                              @PathVariable Long imageId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.deleteImage(id, userDetails.getUserId(), imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/products/" + id + "/edit";
    }

    @PostMapping("/{id}/packages")
    public String addPackage(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             @Valid @ModelAttribute("packageDto") PackageDto packageDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        try {
            productService.addPackage(id, userDetails.getUserId(), packageDto);
            redirectAttributes.addFlashAttribute("successMessage", "Packaging option added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/products/" + id + "/edit";
    }

    @PostMapping("/packages/{packageId}/inventory")
    public String updateInventory(@PathVariable Long packageId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam int quantity,
                                  @RequestParam(defaultValue = "5") int lowStockThreshold,
                                  RedirectAttributes redirectAttributes) {
        try {
            PackageDto dto = new PackageDto();
            dto.setQuantity(quantity);
            dto.setLowStockThreshold(lowStockThreshold);
            productService.updateInventory(packageId, userDetails.getUserId(), quantity, null);
            redirectAttributes.addFlashAttribute("successMessage", "Inventory stock updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/farmer/products";
    }
}
