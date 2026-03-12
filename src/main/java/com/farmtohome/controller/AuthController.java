package com.farmtohome.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.farmtohome.dto.LoginResponse;
import com.farmtohome.model.FarmerProfile;
import com.farmtohome.model.Role;
import com.farmtohome.model.User;
import com.farmtohome.repository.FarmerProfileRepository;
import com.farmtohome.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    private void upsertFarmerProfileFields(User savedUser, Map<String, String> params) {
        if (savedUser == null || savedUser.getRole() != Role.FARMER) return;
        FarmerProfile profile = farmerProfileRepository.findByUserId(savedUser.getId()).orElseGet(() -> {
            FarmerProfile p = new FarmerProfile();
            p.setUser(savedUser);
            return p;
        });
        if (params != null) {
            String farmName = params.get("farmName");
            String state = params.get("state");
            String district = params.get("district");
            String pickupAddress = params.get("pickupAddress");
            if (farmName != null) profile.setFarmName(farmName);
            if (state != null) profile.setState(state);
            if (district != null) profile.setDistrict(district);
            if (pickupAddress != null) profile.setPickupAddress(pickupAddress);
        }
        farmerProfileRepository.save(profile);
    }

    // helper to avoid duplicating logic
    private ResponseEntity<?> performRegistration(User user, Map<String, String> profileParams) {
        try {
            User registeredUser = userService.registerUser(user);
            upsertFarmerProfileFields(registeredUser, profileParams);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error registering user: " + e.getMessage());
        }
    }

    @PostMapping(path = "/register", consumes = "application/json")
    public ResponseEntity<?> registerJson(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setName(body.get("name") == null ? null : body.get("name").toString());
        user.setEmail(body.get("email") == null ? null : body.get("email").toString());
        user.setPassword(body.get("password") == null ? null : body.get("password").toString());
        if (body.get("role") != null) {
            try {
                user.setRole(Role.valueOf(body.get("role").toString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role: " + body.get("role"));
            }
        }
        // Convert role if it's not already set properly
        if (user.getRole() == null && user.getEmail() != null) {
            // Default to CUSTOMER if not specified
            user.setRole(Role.CUSTOMER);
        }
        Map<String, String> profileParams = new java.util.HashMap<>();
        if (body.get("farmName") != null) profileParams.put("farmName", body.get("farmName").toString());
        if (body.get("state") != null) profileParams.put("state", body.get("state").toString());
        if (body.get("district") != null) profileParams.put("district", body.get("district").toString());
        if (body.get("pickupAddress") != null) profileParams.put("pickupAddress", body.get("pickupAddress").toString());
        return performRegistration(user, profileParams);
    }

    @PostMapping(path = "/register", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> registerForm(@RequestParam Map<String, String> params) {
        String name = params.get("name");
        String email = params.get("email");
        String password = params.get("password");
        String roleStr = params.get("role");
        if (name == null || email == null || password == null || roleStr == null) {
            return ResponseEntity.badRequest().body("Missing registration data");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        try {
            user.setRole(Role.valueOf(roleStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role: " + roleStr);
        }
        try {
            User registeredUser = userService.registerUser(user);
            upsertFarmerProfileFields(registeredUser, params);
            // redirect to login page after successful registration
            return ResponseEntity.status(302)
                    .header("Location", "/login.html")
                    .build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error registering user: " + e.getMessage());
        }
    }

    private ResponseEntity<?> performLogin(String userEmail, String userPassword) {
        User user = userService.loginUser(userEmail, userPassword);
        if (user != null) {
            // Convert to LoginResponse to ensure proper JSON serialization
            LoginResponse response = new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().toString() : "CUSTOMER",  // Convert enum to string
                user.getAverageRating(),
                user.getRatingCount()
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping(path = "/login", consumes = "application/json")
    public ResponseEntity<?> loginJson(@RequestBody Map<String, String> loginData) {
        if (loginData == null) {
            return ResponseEntity.badRequest().body("Missing login data");
        }
        return performLogin(loginData.get("email"), loginData.get("password"));
    }

    @PostMapping(path = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> loginForm(@RequestParam Map<String, String> params) {
        String email = params.get("email");
        String password = params.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("Missing login data");
        }

        User user = userService.loginUser(email, password);
        if (user != null) {
            // when using a plain form, redirect to the appropriate page so JSON isn't visible
            // use absolute paths (leading /) so redirect resolves from domain root, not from /api/auth/
            String target = "/index.html";
            if ("FARMER".equals(user.getRole())) {
                target = "/farmer-dashboard.html";
            }
            return ResponseEntity.status(302)
                    .header("Location", target)
                    .build();
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
