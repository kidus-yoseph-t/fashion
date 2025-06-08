package com.project.Fashion.config;

import com.project.Fashion.model.Delivery;
import com.project.Fashion.repository.DeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DeliveryRepository deliveryRepository;

    @Autowired
    public DataInitializer(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // We check if the delivery table is empty before adding data.
        if (deliveryRepository.count() == 0) {
            System.out.println("No delivery options found. Seeding initial data...");

            // Create Delivery Option 1: Standard
            Delivery standard = new Delivery();
            standard.setType("Standard Delivery");
            standard.setDeliveryCost(5.99f);
            // In a real app, you might add a 'deliveryTimeEstimate' field to your Delivery model
            // standard.setDeliveryTimeEstimate("5-7 Business Days");

            // Create Delivery Option 2: Express
            Delivery express = new Delivery();
            express.setType("Express Delivery");
            express.setDeliveryCost(15.99f);
            // express.setDeliveryTimeEstimate("2-3 Business Days");

            // Create Delivery Option 3: Free Shipping
            Delivery free = new Delivery();
            free.setType("Free Shipping");
            free.setDeliveryCost(0.00f);
            // free.setDeliveryTimeEstimate("7-10 Business Days");

            // Save all the new delivery options to the database
            List<Delivery> initialOptions = Arrays.asList(standard, express, free);
            deliveryRepository.saveAll(initialOptions);

            System.out.println("Initial delivery options have been seeded to the database.");
        } else {
            System.out.println("Delivery options already exist. No seeding necessary.");
        }
    }
}