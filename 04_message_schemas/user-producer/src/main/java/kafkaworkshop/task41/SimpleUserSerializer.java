package kafkaworkshop.task41;

import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class SimpleUserSerializer implements Serializer<SimpleUser> {
    private static final Gson gson = new Gson();

    @Override
    public byte[] serialize(String topic, SimpleUser user) {
        return gson.toJson(user).getBytes(StandardCharsets.UTF_8);
    }
}
