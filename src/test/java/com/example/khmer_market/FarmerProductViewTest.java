package com.example.khmer_market;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.PackageStatus;
import com.example.khmer_market.domain.enums.ProductStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.CategoryRepository;
import com.example.khmer_market.repository.ProductPackageRepository;
import com.example.khmer_market.repository.ProductRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FarmerProductViewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Test
    @DisplayName("Verify /farmer/products renders completely including stock, status, actions, and bootstrap js")
    void testFarmerProductsRendersCompletely() throws Exception {
        User farmer = new User();
        farmer.setUsername("testfarmer");
        farmer.setEmail("farmer@test.com");
        farmer.setPassword("Pass1234!");
        farmer.setRole(UserRole.FARMER);
        farmer.setStatus(UserStatus.ACTIVE);
        farmer = userRepository.save(farmer);

        Category cat = new Category();
        cat.setName("Vegetables");
        cat = categoryRepository.save(cat);

        Product product = new Product();
        product.setSeller(farmer);
        product.setCategory(cat);
        product.setName("Organic Carrots");
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(product);
        pkg.setPackageName("Standard Pack");
        pkg.setPrice(new BigDecimal("5.00"));
        pkg.setUnit("kg");
        pkg.setStatus(PackageStatus.ACTIVE);
        pkg.setSku("CARROT-01");
        
        Inventory inv = new Inventory(pkg, 25, 5);
        pkg.setInventory(inv);
        productPackageRepository.save(pkg);

        product.getPackages().add(pkg);
        product = productRepository.save(product);

        CustomUserDetails userDetails = new CustomUserDetails(farmer);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        MvcResult result = mockMvc.perform(get("/farmer/products").with(authentication(auth)))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Check produce name and package
        assertThat(html).contains("Organic Carrots");
        assertThat(html).contains("Standard Pack");
        assertThat(html).contains("25");
        assertThat(html).contains("left");

        // 2. Check Status
        assertThat(html).contains("Active");

        // 3. Check Actions (Edit and Delete buttons)
        assertThat(html).contains("Edit");
        assertThat(html).contains("bi-pencil-square");
        assertThat(html).contains("bi-trash");

        // 4. Check footer and bootstrap script at the bottom of the page
        assertThat(html).contains("bootstrap.bundle.min.js");
        assertThat(html).contains("</html>");
    }
}
