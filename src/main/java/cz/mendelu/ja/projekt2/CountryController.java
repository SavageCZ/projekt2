package cz.mendelu.ja.projekt2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
public class CountryController {
    String getCountryByCode(String countryCode) throws IOException {
        URL url = new URL(String.format("https://restcountries.com/v3.1/alpha/%s?fields=borders,latlng,cca3", countryCode));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();
        Scanner sc = new Scanner(url.openStream());
        String inline = "";
        while (sc.hasNext()) {
            inline += sc.nextLine();
        }
        sc.close();
        return inline;
    }

    int getDirection(LatLng originalPos, LatLng destinationPos) {
        if (destinationPos.x < originalPos.x && destinationPos.y < originalPos.y) return 1;
        else if (destinationPos.x < originalPos.x && destinationPos.y > originalPos.y) return 3;
        else if (destinationPos.x > originalPos.x && destinationPos.y < originalPos.y) return 7;
        else if (destinationPos.x > originalPos.x && destinationPos.y > originalPos.y) return 9;
        else if (destinationPos.x.equals(originalPos.x) && destinationPos.y > originalPos.y) return 6;
        else if (destinationPos.x.equals(originalPos.x) && destinationPos.y < originalPos.y) return 4;
        else if (destinationPos.x > originalPos.x && destinationPos.y.equals(originalPos.y)) return 8;
        else if (destinationPos.x < originalPos.x && destinationPos.y.equals(originalPos.y)) return 2;
        return 0;
    }

    TraceResult findBestPath(String originCountryCode, CountryInfo destinationInfo, AtomicInteger hopCount, List<String> processedCountries) throws IOException {
        String destinationCountryCode = destinationInfo.cca3;
        log.info("Processing {} -> {}", originCountryCode, destinationCountryCode);
        ObjectMapper mapper = new ObjectMapper();
        CountryInfo originInfo = mapper.readValue(getCountryByCode(originCountryCode), CountryInfo.class);
        int desiredDirection = getDirection(originInfo.getPosition(), destinationInfo.getPosition());
        if (!processedCountries.contains(originCountryCode)) processedCountries.add(originCountryCode);
        hopCount.incrementAndGet();
        boolean directionWasFound = false;
        if (originInfo.borders.contains(destinationCountryCode)) {
            processedCountries.add(destinationCountryCode);
        } else {
            List<String> testedCountries = new ArrayList<String>();
            for (String nextCountry : originInfo.borders) {
                if (!processedCountries.contains(nextCountry)) {
                    testedCountries.add(nextCountry);
                    int direction = getDirection(originInfo.getPosition(), mapper.readValue(getCountryByCode(nextCountry), CountryInfo.class).getPosition());
                    if (direction == desiredDirection) {
                        log.info("Tenhle to je {}", nextCountry);
                        directionWasFound = true;
                        findBestPath(nextCountry, destinationInfo, hopCount, processedCountries);
                        break;
                    }
                }
            }
            if (!directionWasFound) {
                List<String> foundCountries = new ArrayList<String>();
                boolean testedFound = false;
                for (String nextCountry : testedCountries) {
                    int direction = getDirection(originInfo.getPosition(), mapper.readValue(getCountryByCode(nextCountry), CountryInfo.class).getPosition());
                    if (direction == desiredDirection - 3 || direction == desiredDirection + 3 || direction == desiredDirection - 6 || direction == desiredDirection + 6) {
                        log.info("Tak nakonec beru {}", nextCountry);
                        testedFound = true;
                        findBestPath(nextCountry, destinationInfo, hopCount, processedCountries);
                        break;
                    } else {
                        foundCountries.add(nextCountry);
                    }
                }
                if (!testedFound) {
                    for (String nextCountry : foundCountries) {
                        findBestPath(nextCountry, destinationInfo, hopCount, processedCountries);
                    }
                }
                return new TraceResult(processedCountries, hopCount);
            }
        }
        return new TraceResult(processedCountries, hopCount);
    }

    TraceResult getShortestPathForNode(String origin, String destination) throws IOException {
        if (origin.equals(destination)) return new TraceResult(new ArrayList<String>() {{
            add(origin);
            add(destination);
        }}, new AtomicInteger(0));
        AtomicInteger hopCount = new AtomicInteger(0);
        List<String> processedCountries = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        CountryInfo destinationInfo = mapper.readValue(getCountryByCode(destination), CountryInfo.class);
        return findBestPath(origin, destinationInfo, hopCount, processedCountries);
    }
    List<OriginDestinationPair> parseOriginDestinationFile(InputStream is){
        List<OriginDestinationPair> list = new ArrayList<OriginDestinationPair>();
        Scanner sc = new Scanner(is);
        while (sc.hasNextLine()) {
            var line=sc.nextLine();
            list.add(new OriginDestinationPair(line.split(",")[0],line.split(",")[1]));
        }
        sc.close();
        return list;
    }
    String formatOutput(List<TraceResult> resultList){
        StringBuilder sb = new StringBuilder();
        for (TraceResult result:
             resultList) {
            sb.append(String.format("Best path for package from %s to %s is %d hops via ",result.countries.get(0),result.countries.get(result.countries.size()-1),result.hopCount.get()));
            for(String country : result.countries)
            {
                sb.append(String.format("%s->",country));
            }
            sb.delete(sb.length()-2,sb.length());
            sb.append(".\n");
        }
        sb.delete(sb.length()-1,sb.length());
        return sb.toString();
    }

    @GetMapping(path = "/trace")
    TraceResult test(@RequestParam String origin, @RequestParam String destination) throws IOException {
        return getShortestPathForNode(origin,destination);

        //return "";
    }

    @PostMapping(path = "/trace")
    String post(HttpServletRequest request) throws IOException, ServletException {
        var list = parseOriginDestinationFile(request.getPart("file").getInputStream());
        List<TraceResult> resultList = new ArrayList<TraceResult>();
        for (OriginDestinationPair pair : list)
        {
            resultList.add(getShortestPathForNode(pair.origin,pair.destination));
        }
        return formatOutput(resultList);
    }
}
