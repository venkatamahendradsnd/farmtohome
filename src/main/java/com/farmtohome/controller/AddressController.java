package com.farmtohome.controller;

import com.farmtohome.model.Address;
import com.farmtohome.model.User;
import com.farmtohome.repository.AddressRepository;
import com.farmtohome.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Address>> getAddresses(@PathVariable Long customerId) {
        return ResponseEntity.ok(addressRepository.findByCustomerId(customerId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAddress(@RequestBody Address address, @RequestParam Long customerId) {
        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null)
            return ResponseEntity.badRequest().body("Customer not found");

        address.setCustomer(customer);
        return ResponseEntity.ok(addressRepository.save(address));
    }
}
