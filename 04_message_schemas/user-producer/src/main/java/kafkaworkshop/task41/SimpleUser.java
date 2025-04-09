package kafkaworkshop.task41;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleUser {
    public enum Color {
        red, yellow, blue, green
    }

    private String name;
    private int age;
    @SerializedName("favorite_color")
    private Color favoriteColor;
}
