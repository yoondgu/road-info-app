package util;

/**
 * 두 좌표간의 거리를 계산하는 HaversinDistance 클래스
 * @author doyoung
 *
 */
public class HaversineDistance {
	
	final int R = 6371; // Radious of the earth (km)
	private double distance;
	
	public HaversineDistance(double lat1, double lon1, double lat2, double lon2) {
		Double latDistance = toRad(lat2-lat1);
		Double lonDistance = toRad(lon2-lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
				Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * 
				Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		
		this.distance = R * c;
//		System.out.println("The distance between two lat and long is:" + distance);
	}
	 
	 private static Double toRad(Double value) {
	 return value * Math.PI / 180;
	 }
	 
	 public double getDistance() {
		return distance;
	}
}
