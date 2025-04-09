package kafkaworkshop.task46;

import kafkaworkshop.User;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroWithoutRegistryUserSerializer implements Serializer<User> {
    @Override
    public byte[] serialize(String topic, User user) {
        if (user == null) {
            return null;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null);

            DatumWriter<User> datumWriter = new SpecificDatumWriter<>(User.class);
            datumWriter.write(user, binaryEncoder);

            binaryEncoder.flush();
            byteArrayOutputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Unable to serialize data='" + user + "' for topic='" + topic + "'", ex);
        }
    }
}
