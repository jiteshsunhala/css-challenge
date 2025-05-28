package com.css.challenge;

import com.css.challenge.config.KitchenConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = KitchenConfiguration.class)
public class KitchenApplication {

  public static void main(String[] args) {
    SpringApplication.run(KitchenApplication.class, args);
  }
}
