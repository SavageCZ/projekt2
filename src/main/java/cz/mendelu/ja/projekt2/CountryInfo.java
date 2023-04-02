package cz.mendelu.ja.projekt2;

import java.util.List;
public class CountryInfo {
    public List<String> borders;
    public Double[] latlng;
    public String cca3;
    public LatLng getPosition(){
        return new LatLng(latlng);
    }
}
