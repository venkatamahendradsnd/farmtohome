package com.farmtohome.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.farmtohome.model.FarmerProfile;
import com.farmtohome.model.Role;
import com.farmtohome.model.User;
import com.farmtohome.repository.FarmerProfileRepository;
import com.farmtohome.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    public User registerUser(User user) {
        // In real app, hash password here
        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == Role.FARMER) {
            FarmerProfile profile = new FarmerProfile();
            profile.setUser(savedUser);
            profile.setRating(3.0); // Default rating
            profile.setTotalReviews(0);
            profile.setVerified(false);
            farmerProfileRepository.save(profile);
        }

        return savedUser;
    }

    public User loginUser(String email, String password) {
        System.out.println("Attempting login for email=" + email + " password=" + password);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            System.out.println("Found user with password=" + user.get().getPassword());
            if (user.get().getPassword().equals(password)) {
                System.out.println("Password match successful");
                return user.get();
            } else {
                System.out.println("Password mismatch");
            }
        } else {
            System.out.println("No user found with that email");
        }
        return null;
    }
}
