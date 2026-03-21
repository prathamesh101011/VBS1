package com.vbs.demo.controller;

import com.vbs.demo.dto.TransactionDto;
import com.vbs.demo.dto.TransferDto;
import com.vbs.demo.models.Notification;
import com.vbs.demo.models.Transaction;
import com.vbs.demo.models.User;
import com.vbs.demo.repositories.TransactionRepo;
import com.vbs.demo.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class TransactionController {
    @Autowired //springboot creates object of interface by using @Autowired (java cannot
    // create object of interface)
    TransactionRepo transactionRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    NotificationRepo notificationRepo;

    @PostMapping("/deposit")
    public String deposit(@RequestBody TransactionDto obj)
    {
        User user = userRepo.findById(obj.getId()).orElseThrow(()->new RuntimeException("User not found"));
        double newBalance = user.getBalance() + obj.getAmount();
        user.setBalance(newBalance);
        userRepo.save(user);

        Transaction t = new Transaction();
        t.setAmount(obj.getAmount());
        t.setCurrBalance(newBalance);
        t.setDescription("Rs"+ obj.getAmount()+" Deposit Successful");
        t.setUserId(obj.getId());
        transactionRepo.save(t);
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage("Rs " + obj.getAmount() + " deposited successfully");
        n.setCreatedAt(java.time.LocalDateTime.now());
        n.setRead(false);
        notificationRepo.save(n);
        return "Deposit Successful";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody TransactionDto obj)
    {
        User user = userRepo.findById(obj.getId()).orElseThrow(()->new RuntimeException("User not found"));
        double newBalance = user.getBalance() - obj.getAmount();
        if(newBalance < 0)
        {
            return "Insufficient Balance";
        }
        user.setBalance(newBalance);
        userRepo.save(user);

        Transaction t = new Transaction();
        t.setAmount(obj.getAmount());
        t.setCurrBalance(newBalance);
        t.setDescription("Rs"+ obj.getAmount()+" Withdrawal Successful");
        t.setUserId(obj.getId());
        transactionRepo.save(t);
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage("Rs " + obj.getAmount() + " withdrawn successfully");
        n.setCreatedAt(java.time.LocalDateTime.now());
        n.setRead(false);
        notificationRepo.save(n);
        return "Withdrawal Successful";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferDto obj)
    {
        User sender = userRepo.findById(obj.getId())
                .orElseThrow(()->new RuntimeException("Not found"));
        User rec = userRepo.findByUsername(obj.getUsername());
        if(rec == null) {return("Receiver not found");}
        if(sender.getId() == rec.getId()) {return ("Self transaction not allowed");}
        if(obj.getAmount() < 1) {return "Invalid amount";}

        double sbalance = sender.getBalance() - obj.getAmount();
        if(sbalance < 0) {return "Insufficient Balance";}
        double rbalance = rec.getBalance() + obj.getAmount();

        sender.setBalance(sbalance);
        rec.setBalance(rbalance);
        userRepo.save(sender);
        userRepo.save(rec);

        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();

        t1.setAmount(obj.getAmount());
        t1.setCurrBalance(sbalance);
        t1.setDescription("Rs "+obj.getAmount()+" Sent to user "+obj.getUsername());
        t1.setUserId(sender.getId());

        t2.setAmount(obj.getAmount());
        t2.setCurrBalance(rbalance);
        t2.setDescription("Rs "+obj.getAmount()+" Received from user "+obj.getUsername());
        t2.setUserId(rec.getId());

        transactionRepo.save(t1);
        transactionRepo.save(t2);
        Notification n1 = new Notification();
        n1.setUser(sender);
        n1.setMessage("Rs " + obj.getAmount() + " sent to " + rec.getUsername());
        n1.setCreatedAt(java.time.LocalDateTime.now());
        n1.setRead(false);

        notificationRepo.save(n1);
        Notification n2 = new Notification();
        n2.setUser(rec);
        n2.setMessage("Rs " + obj.getAmount() + " received from " + sender.getUsername());
        n2.setCreatedAt(java.time.LocalDateTime.now());
        n2.setRead(false);

        notificationRepo.save(n2);
        return "Transfer Done SuccessfulLy";
    }

    @GetMapping("/passbook/{id}")
    public List<Transaction> getPassbook(@PathVariable int id)
    {
        return transactionRepo.findAllByUserId(id);
    }
}
