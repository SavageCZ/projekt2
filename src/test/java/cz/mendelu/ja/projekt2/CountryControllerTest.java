package cz.mendelu.ja.projekt2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountryControllerTest {

    private final CountryController controller = new CountryController();

    @Test
    void testGetShortestPathForNode() throws IOException {
        String origin = "CZE";
        String destination = "ITA";
        List<String> expectedCountries = new ArrayList<String>() {{
            add("CZE");
            add("AUT");
            add("ITA");
        }};
        AtomicInteger expectedHops = new AtomicInteger(2);
        TraceResult expected = new TraceResult(expectedCountries, expectedHops);
        TraceResult actual = controller.getShortestPathForNode(origin, destination);
        assertEquals(expected.getCountries(), actual.getCountries());
        assertEquals(expected.getHopCount().get(), actual.getHopCount().get());
    }

}
