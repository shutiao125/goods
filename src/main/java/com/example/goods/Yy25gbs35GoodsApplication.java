package com.example.goods;

import com.example.goods.Resovel.MyLocalResovel;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;

@SpringBootApplication
@MapperScan("com.example.goods.mapper")
@EnableCaching
@EnableScheduling
public class Yy25gbs35GoodsApplication {
    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocalResovel();
    }

    public static void main(String[] args) {
        SpringApplication.run(Yy25gbs35GoodsApplication.class, args);
    }


}