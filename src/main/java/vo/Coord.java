package vo;

public class Coord {

	private int csvIdx; // csv파일의 행순서 (0번부터 저장, csvIdx가 0이면 csv파일의 2번째 행이다.)
	private double latitude;
	private double longitude;
	
	public Coord(double latitude, double longitude, int csvIdx) {
		this.csvIdx = csvIdx;
		this.latitude = latitude;
		this.longitude = longitude;
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
