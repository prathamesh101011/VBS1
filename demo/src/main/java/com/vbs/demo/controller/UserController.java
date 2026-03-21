package com.vbs.demo.controller;

import com.vbs.demo.dto.DisplayDto;
import com.vbs.demo.dto.LoginDto;
import com.vbs.demo.dto.UpdateDto;
import com.vbs.demo.models.History;
import com.vbs.demo.models.User;
import com.vbs.demo.repositories.HistoryRepo;
import com.vbs.demo.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    HistoryRepo historyRepo;

    @PostConstruct
    public void createDefaultAdmin() {
        if (userRepo.findByRole("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRole("admin");
            admin.setName("Main Admin");
            admin.setEmail("admin@mail.com");
            admin.setBalance(0);
            userRepo.save(admin);
        }
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {

        if (user.getRole().equalsIgnoreCase("admin")) {
            return "You cannot register as admin";
        }

        userRepo.save(user);

        History h1 = new History();
        h1.setDescription("User Self Created : " + user.getUsername());
        historyRepo.save(h1);

        return "Signup Successful";
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginDto u) {
        User user = userRepo.findByUsername(u.getUsername());

        if (user == null) return "User Not Found";

        if (!u.getPassword().equals(user.getPassword()))
            return "Password Incorrect";

        if (!u.getRole().equalsIgnoreCase(user.getRole()))
            return "Role Incorrect";

        return String.valueOf(user.getId());
    }

    @GetMapping("/get-details/{id}")
    public DisplayDto displayDto(@PathVariable int id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DisplayDto displayDto = new DisplayDto();
        displayDto.setUsername(user.getUsername());
        displayDto.setBalance(user.getBalance());

        return displayDto;
    }

    @PostMapping("/update")
    public String update(@RequestBody UpdateDto obj) {

        User user = userRepo.findById(obj.getId())
                .orElseThrow(() -> new RuntimeException("Not Found"));

        History h1 = new History();

        if (obj.getKey().equalsIgnoreCase("name")) {
            if (user.getName().equalsIgnoreCase(obj.getValue()))
                return "Cannot be same";

            h1.setDescription("User changed Name from " + user.getName() + " to " + obj.getValue());
            user.setName(obj.getValue());
        }

        else if (obj.getKey().equalsIgnoreCase("password")) {
            if (user.getPassword().equalsIgnoreCase(obj.getValue()))
                return "Cannot be same";

            h1.setDescription("User changed Password : " + user.getUsername());
            user.setPassword(obj.getValue());
        }

        else if (obj.getKey().equalsIgnoreCase("email")) {
            if (user.getEmail().equalsIgnoreCase(obj.getValue()))
                return "Cannot be same";

            User user2 = userRepo.findByEmail(obj.getValue());
            if (user2 != null) return "Email already exists";

            h1.setDescription("User changed Email from " + user.getEmail() + " to " + obj.getValue());
            user.setEmail(obj.getValue());
        }

        else {
            return "Invalid Key";
        }

        historyRepo.save(h1);
        userRepo.save(user);

        return "Update done Successfully";
    }

    @PostMapping("/add/{adminId}")
    public String add(@RequestBody User user, @PathVariable int adminId) {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!admin.getRole().equalsIgnoreCase("admin")) {
            return "Only admin can add users";
        }

        if (user.getRole().equalsIgnoreCase("admin")) {
            return "Admin already exists. Cannot create another admin.";
        }

        History h1 = new History();
        h1.setDescription("User " + user.getUsername() + " Created By admin : " + adminId);
        historyRepo.save(h1);

        userRepo.save(user);

        return "Successfully added";
    }

    @GetMapping("/users")
    public List<User> getAllUsers(@RequestParam String sortBy,
                                  @RequestParam String order) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return userRepo.findAllByRole("customer", sort);
    }

    @GetMapping("/users/{keyword}")
    public List<User> getUsers(@PathVariable String keyword) {
        return userRepo.findByUsernameContainingIgnoreCaseAndRole(keyword, "customer");
    }

    @DeleteMapping("delete-user/{userId}/admin/{adminId}")
    public String deleteUser(@PathVariable int userId,
                             @PathVariable int adminId) {

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!admin.getRole().equalsIgnoreCase("admin")) {
            return "Only admin can delete users";
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getBalance() > 0) {
            return "Balance should be zero";
        }

        History h1 = new History();
        h1.setDescription("User " + user.getUsername() + " Deleted By admin : " + adminId);
        historyRepo.save(h1);

        userRepo.delete(user);

        return "User Deleted Successfully";
    }
}