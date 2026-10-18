package com.example.khmer_market.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FarmerProfileDto {

    @NotBlank(message = "Farm name is required")
    @Size(max = 150, message = "Farm name must not exceed 150 characters")
    private String farmName;

    @NotBlank(message = "Farmer representative name is required")
    @Size(max = 150, message = "Farmer name must not exceed 150 characters")
    private String farmerName;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 100, message = "Address must not exceed 100 characters")
    private String address;

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 5000, message = "Description is too long")
    private String farmDescription;

    private String farmImage;
    private String username;

    public FarmerProfileDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getFarmDescription() { return farmDescription; }
    public void setFarmDescription(String farmDescription) { this.farmDescription = farmDescription; }

    public String getFarmImage() { return farmImage; }
    public void setFarmImage(String farmImage) { this.farmImage = farmImage; }
}
