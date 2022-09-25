package dto;

import vo.Coord;

public class MatchedPoint {

	private Coord sourceLocation;
	private String idxName;
	private String linkId;
	private int speed;
	private String roadCategoryName;
	
	public void setIdxName(String idxName) {
		this.idxName = idxName;
	}

	public void setSourceLocation(Coord sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}
	
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	public void setRoadCategoryName(String roadCategoryName) {
		this.roadCategoryName = roadCategoryName;
	}
	
}
