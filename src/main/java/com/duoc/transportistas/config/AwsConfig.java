package com.duoc.transportistas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        // Configuramos el cliente apuntando a la región de Virginia (us-east-1)
        // El SDK buscará automáticamente tus credenciales en las variables de entorno de la EC2
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}