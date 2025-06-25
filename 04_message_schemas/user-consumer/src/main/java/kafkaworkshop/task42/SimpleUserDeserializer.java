package kafkaworkshop.task42;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.serialization.Deserializer;

public class SimpleUserDeserializer implements Deserializer<SimpleUser> {
    private static final Gson gson = new Gson();

    @Override
    public SimpleUser deserialize(String topic, byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), SimpleUser.class);
    }
}
