import game.Chunk;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenerationStressTest {

    @Test
    void testGenerationTime(){
        long startTime = System.nanoTime();
        for(int i=0; i<8; i++){
            new Chunk(new Vector3i(i));
        }
        long endTime = System.nanoTime();
        //Assertions.assertTrue((endTime - startTime) < 5000000);
    }

}
