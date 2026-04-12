package com.demo.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/transactions")
    public List<Map<String, Object>> getTransactions() {
        return List.of(
            Map.of("id", "TXN-84729", "amount", 4500.00, "status", "CLEARED", "risk", "LOW"),
            Map.of("id", "TXN-84730", "amount", 12.50,   "status", "PENDING", "risk", "LOW"),
            Map.of("id", "TXN-84731", "amount", 89000.00,"status", "BLOCKED", "risk", "CRITICAL"),
            Map.of("id", "TXN-84732", "amount", 340.00,  "status", "CLEARED", "risk", "MEDIUM")
        );
    }
    
    @GetMapping("/api/system")
    public Map<String, String> getSystemStatus() {
        return Map.of(
            "status", "SECURE", 
            "uptime", "99.99%", 
            "version", "v2.1.0-Release",
            "lastScan", "Passed"
        );
    }
}
