package vo;

public class Coord {

	private double latitude;
	private double longitude;
	private int csvIdx; // csv파일의 행순서 (0번부터 저장, csvIdx가 0이면 csv파일의 2번째 행이다.)
	
	public Coord(double latitude, double longitude, int csvIdx) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.csvIdx = csvIdx;
	}
	
	public int getCsvIdx() {
		return csvIdx;
	}

	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}

	@Override
	public String toString() {
		return "Coord [latitude=" + latitude + ", longitude=" + longitude + "]";
	}
	
}
