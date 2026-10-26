package com.example.khmer_market.web.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class BecomeSellerDto {

    @NotBlank(message = "Full legal name matching government ID is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String farmerName;

    @NotNull(message = "Date of birth is required for seller verification")
    @Past(message = "Date of birth must be a valid past date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    @NotBlank(message = "ID document type is required")
    private String idType = "National ID Card"; // 'National ID Card', 'Passport', 'Driver License'

    @NotBlank(message = "Government ID / Passport number is required")
    @Size(max = 100, message = "ID number must not exceed 100 characters")
    private String idNumber;

    @NotBlank(message = "Farm or shop name is required")
    @Size(max = 150, message = "Farm name must not exceed 150 characters")
    private String farmName;

    @NotBlank(message = "Province is required")
    private String province = "Phnom Penh";

    @NotBlank(message = "Farm or business address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "Direct contact phone number is required")
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String farmDescription;

    public BecomeSellerDto() {}

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFarmDescription() { return farmDescription; }
    public void setFarmDescription(String farmDescription) { this.farmDescription = farmDescription; }
}
