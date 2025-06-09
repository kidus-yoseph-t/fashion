package com.project.Fashion.config;

import com.project.Fashion.model.Delivery;
import com.project.Fashion.repository.DeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DeliveryRepository deliveryRepository;

    @Autowired
    public DataInitializer(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking and initializing delivery options...");

        // Define the delivery options and their properties
        upsertDeliveryOption("Standard Delivery", 5.99f, 5, 7);
        upsertDeliveryOption("Express Delivery", 15.99f, 2, 3);
        upsertDeliveryOption("Free Shipping", 0.00f, 7, 10);

        System.out.println("Delivery options are up to date.");
    }

    /**
     * Creates a delivery option if it doesn't exist, or updates it
     * if it's missing the delivery day information.
     */
    private void upsertDeliveryOption(String type, float cost, int minDays, int maxDays) {
        Optional<Delivery> existingOptionOpt = deliveryRepository.findByType(type);

        if (existingOptionOpt.isEmpty()) {
            // Option doesn't exist, so create it
            System.out.println("Creating new delivery option: " + type);
            Delivery newOption = new Delivery();
            newOption.setType(type);
            newOption.setDeliveryCost(cost);
            newOption.setMinDeliveryDays(minDays);
            newOption.setMaxDeliveryDays(maxDays);
            deliveryRepository.save(newOption);
        } else {
            // Option exists, check if it needs to be updated
            Delivery existingOption = existingOptionOpt.get();
            if (existingOption.getMinDeliveryDays() == null || existingOption.getMaxDeliveryDays() == null) {
                System.out.println("Updating existing delivery option with delivery days: " + type);
                existingOption.setMinDeliveryDays(minDays);
                existingOption.setMaxDeliveryDays(maxDays);
                deliveryRepository.save(existingOption);
            }
        }
    }
}