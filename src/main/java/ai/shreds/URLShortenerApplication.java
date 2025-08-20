package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
    }
)
public class URLShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(URLShortenerApplication.class, args);
    }

}
