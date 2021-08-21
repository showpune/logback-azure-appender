package person.zhiyongli.azure.appender.sample;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "appender.sample")
public class SampleProperties {
}
