package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Category;
import com.example.khmer_market.service.CatalogService;
import com.example.khmer_market.service.CategoryService;
import com.example.khmer_market.web.dto.ProductCardDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final CatalogService catalogService;
    private final CategoryService categoryService;

    public HomeController(CatalogService catalogService, CategoryService categoryService) {
        this.catalogService = catalogService;
        this.categoryService = categoryService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        List<ProductCardDto> featuredProducts = catalogService.getFeaturedProducts(8);
        List<Category> categories = categoryService.findAllActive();

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("categories", categories);

        return "public/home";
    }
}
