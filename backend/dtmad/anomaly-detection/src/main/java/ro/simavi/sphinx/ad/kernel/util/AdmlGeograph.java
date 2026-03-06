package ro.simavi.sphinx.ad.kernel.util;

public class AdmlGeograph {

    private static double R = 6372.8; //radius in km

    public Double haversineDistance(Double lat1, Double lon1, Double lat2, Double lon2){
        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);

        Double a = Math.pow(Math.sin(dLat/2),2) + Math.pow(Math.sin(dLon/2),2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        Double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    public Double haversineDistanceFromStrings(String coords1, String coords2){
        try {
            String[] coordsDouble1 = coords1.split(",");
            String[] coordsDouble2 = coords1.split(",");

            return haversineDistance(getCoordsDouble(coordsDouble1[0]),
                    getCoordsDouble(coordsDouble1[1]),
                    getCoordsDouble(coordsDouble2[0]),
                    getCoordsDouble(coordsDouble2[1]));
        } catch (Throwable t){
            return 999999999D;
        }
    }

    private Double getCoordsDouble(String coords){
        return Double.parseDouble(coords);
    }
}
