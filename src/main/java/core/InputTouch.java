package core;

import lombok.Data;

@Data
public class InputTouch {

    private String name;
    private int touch;

    public InputTouch(String name, int touch) {
        this.name = name;
        this.touch = touch;
    }
}
